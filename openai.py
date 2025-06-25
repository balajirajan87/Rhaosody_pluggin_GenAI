import requests
import time
import json
import fitz  # PyMuPDF
from chromadb import Client, Embeddings, EmbeddingFunction, PersistentClient
from chromadb.config import Settings
import numpy as np
import hashlib
import os
from PIL import Image
from flask import Flask, request, jsonify
import io
from flask_cors import CORS
import base64

# Constants for Embedding and LLM API
TEXT_EMBEDDING_MODEL = "text-embedding-3-small"
IMAGE_EMBEDDING_MODEL = "image-embedding-1"
CHAT_MODEL = "gpt-4.1"
API_URL = 'https://openaichatgpt-ms-epb1-xc.openai.azure.com/openai/deployments'
API_KEY = os.getenv('OPENAI_API_KEY')  # Read API key from environment variable
CHAT_API_VERSION = "2025-01-01-preview"
EMBEDDING_API_VERSION = "2024-02-01"

if not API_KEY:
    raise ValueError("API key not found. Please set the OPENAI_API_KEY environment variable.")

HEADERS = {
    'Authorization': f"Bearer {API_KEY}",
    'Content-Type': 'application/json'
}
REQUIREMENT_METADATA_FILE = "Requirement_pdf_metadata.json"
REFERENCE_DATA_METADATA_FILE = "Reference_pdf_metadata.json"
GUIDELINE_DATA_METADATA_FILE = "Guideline_pdf_metadata.json"
REFERENCE_CODE_METADATA_FILE = "Reference_Code_metadata.json"

app = Flask(__name__)
CORS(app)  # Enable CORS for cross-origin requests

# In-memory storage for chat contexts
chat_contexts = {}

print(f"Current working directory: {os.getcwd()}")

# Function to calculate MD5 hash of a PDF file
def calculate_pdf_hash(pdf_path):
    """
    Calculate the MD5 hash of a PDF file.

    Args:
        pdf_path (str): The file path to the PDF file.

    Returns:
        str: The MD5 hash of the PDF file as a hexadecimal string.

    Example:
        >>> calculate_pdf_hash('example.pdf')
        'd41d8cd98f00b204e9800998ecf8427e'
    """
    hasher = hashlib.md5()
    with open(pdf_path, 'rb') as f:
        buf = f.read()
        hasher.update(buf)
    return hasher.hexdigest()

# Function to extract text from PDF
def extract_text_from_pdf(pdf_path):
    """
    Extract text from a PDF file.

    Args:
        pdf_path (str): The file path to the PDF file.

    Returns:
        str: The extracted text from the PDF file.

    Example:
        >>> extract_text_from_pdf('example.pdf')
        'This is the extracted text from the PDF file.'
    """
    doc = fitz.open(pdf_path)
    text = ""
    for page in doc:
        text += page.get_text()
    doc.close()
    return text

def read_source_code(file_path):
    """
    Reads the content of a source code file and returns it as a string.
    Args:
        file_path (str): The path to the file to be read.
    Returns:
        str: The content of the file as a string.
    Raises:
        FileNotFoundError: If the file does not exist.
        IOError: If there is an error reading the file.
    """
    with open(file_path, 'r', encoding='utf-8') as f:
        return f.read()

# Function to extract images from PDF
def extract_images_from_pdf(pdf_path):
    """
    Extract images from a PDF file.

    Args:
        pdf_path (str): The file path to the PDF file.

    Returns:
        list: A list of PIL Image objects extracted from the PDF file.
    """
    doc = fitz.open(pdf_path)
    images = []
    for page in doc:
        for img in page.get_images(full=True):
            xref = img[0]
            base_image = doc.extract_image(xref)
            image_bytes = base_image["image"]
            image = Image.open(io.BytesIO(image_bytes))
            images.append(image)
    doc.close()
    return images

# Split text into chunks
def split_text_into_chunks(text, chunk_size=1000):
    """
    Split text into chunks of a specified size.

    Args:
        text (str): The text to be split into chunks.
        chunk_size (int, optional): The size of each chunk. Default is 1000.

    Returns:
        list: A list of text chunks.

    Example:
        >>> split_text_into_chunks('This is a long text that needs to be split into chunks.', chunk_size=10)
        ['This is a ', 'long text ', 'that needs', ' to be spl', 'it into ch', 'unks.']
    """
    return [text[i:i+chunk_size] for i in range(0, len(text), chunk_size)]

# Embedding Function Class with Rate Limiting
class MyEmbeddingFunction(EmbeddingFunction):
    """
    A class to handle embedding functions with rate limiting.

    Attributes:
        batch_size (int): The size of each batch for processing.

    Methods:
        get_embedding(batch_data):
            Sends a batch of document chunk request to the embedding API and returns the embeddings for the chunks.
        
        __call__(chunks):
            Processes chunks in batches to reduce the number of API calls.
        
        __get_user_querry_embedding__(user_querry):
            Retrieves the embedding for a single user query.
    """
    def __init__(self, batch_size=5, max_retries=5, backoff_factor=2):
        """
        Initializes the MyEmbeddingFunction with a specified batch size.

        Args:
            batch_size (int): The size of each batch for processing. Default is 5.
        """
        self.batch_size = batch_size
        self.max_retries = max_retries
        self.backoff_factor = backoff_factor

    def get_embedding(self, batch_data):
        """
        Helper function to send a batch request to the embedding API.

        Args:
            batch_data (list): A list of inputs for batch processing.

        Returns:
            list: A list of embeddings if successful, otherwise None.
        """
        data = {
            'input': batch_data,  # Sending a list of inputs for batch processing
            'dimensions': 1024
        }
        
        for attempt in range(self.max_retries):
            try:
                response = requests.post(f'{API_URL}/{TEXT_EMBEDDING_MODEL}/embeddings?api-version={EMBEDDING_API_VERSION}', headers=HEADERS, json=data)
                response.raise_for_status()  # Raises an HTTPError if the response was unsuccessful
                data_ = response.json()
                
                # Debugging: Print the response for troubleshooting
                # print("API Response:", data_)

                # Extract embeddings if present
                if 'data' in data_:
                    # Flatten and validate
                    embeddings = [item.get('embedding') for item in data_['data']]
                    return embeddings
                
                print("Embedding not found in response")
                return None
            except requests.exceptions.RequestException as e:
                if 'response' in locals() and response.status_code == 429:
                    # Handle rate limiting
                    print(f"Rate limit exceeded. Retrying in {self.backoff_factor * (2 ** attempt)} seconds...")
                    time.sleep(self.backoff_factor * (2 ** attempt))
                else:
                    print(f"Request failed: {e}")
                    return None
            except KeyError as e:
                print(f"KeyError in response: {e}")
                return None
        
    def get_image_embedding(self, images):
        """
        Helper function to send a batch of image data to the embedding API.

        Args:
            images (list): A list of PIL Image objects.

        Returns:
            list: A list of embeddings if successful, otherwise None.
        """
        descriptions = []

        for image in images:
            # Convert the image to base64
            buffered = io.BytesIO()
            image.save(buffered, format="PNG")
            image_base64 = base64.b64encode(buffered.getvalue()).decode("utf-8")

            # Compose the payload
            data = {
                "messages": [
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "Describe this image in detail. Make sure to include all the important details like text. If the image contains UML diagrams, understand and describe its components."
                            },
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": f"data:image/png;base64,{image_base64}",
                                    "detail": "high"
                                }
                            }
                        ]
                    }
                ],
                "max_tokens": 500,
                "temperature": 0.5,
                "top_p": 0.9
            }

            try:
                response = requests.post(
                    f'{API_URL}/{CHAT_MODEL}/chat/completions?api-version={CHAT_API_VERSION}',
                    headers=HEADERS,
                    json=data
                )
                response.raise_for_status()
                data_ = response.json()
                if 'choices' in data_ and len(data_['choices']) > 0:
                    descriptions.append(data_['choices'][0]['message']['content'])
                else:
                    descriptions.append(None)
            except requests.exceptions.RequestException as e:
                print(f"Failed to describe image: {e}")
                descriptions.append(None)

        # Generate embeddings for the descriptions
        text_embeddings = []
        for description in descriptions:
            if description:
                text_embedding = self.__get_user_querry_embedding__(description)
                text_embeddings.append(text_embedding)
            else:
                text_embeddings.append(None)

        return text_embeddings

    def __call__(self, chunks):
        """
        Process chunks in batches to reduce the number of API calls.

        Args:
            chunks (list): A list of text chunks to be processed.

        Returns:
            list: A list of embeddings corresponding to the chunks.
        """
        embeddings = []
        for i in range(0, len(chunks), self.batch_size):
            batch = chunks[i:i + self.batch_size]
            result = self.get_embedding(batch)
            if result:
                embeddings.extend(result)
            else:
                # If batch processing fails, fill with None to maintain alignment
                embeddings.extend([None] * len(batch))
        return embeddings

    def __get_user_querry_embedding__(self, user_querry: str) -> Embeddings:
        """
        Retrieves the embedding for a single user query.

        Args:
            user_querry (str): The user query string.

        Returns:
            list: The embedding for the user query as a list of floats, or None if unsuccessful.
        """
        data = {
            'input': user_querry,
            'dimensions': 1024
        }
        
        for attempt in range(self.max_retries):
            try:
                response = requests.post(f'{API_URL}/{TEXT_EMBEDDING_MODEL}/embeddings?api-version={EMBEDDING_API_VERSION}', headers=HEADERS, json=data)
                response.raise_for_status()
                data_ = response.json()
                if 'data' in data_ and len(data_['data']) > 0 and 'embedding' in data_['data'][0]:
                    return data_['data'][0]['embedding']
                return None
            except requests.exceptions.RequestException as e:
                if response.status_code == 429:
                    # Handle rate limiting
                    print(f"Rate limit exceeded. Retrying in {self.backoff_factor * (2 ** attempt)} seconds...")
                    time.sleep(self.backoff_factor * (2 ** attempt))
                else:
                    print(f"Request failed: {e}")
                    return None
            except KeyError as e:
                print(f"KeyError in response: {e}")
                return None

# Initialize Chroma Client (new method)
def init_chroma_client(collection_name):
    """
    Initializes the Chroma client with default settings and ensures the collection is created or retrieved.

    Returns:
        client: The initialized Chroma client.
        collection: The Chroma collection for storing embeddings.

    Example:
        client, collection = init_chroma_client()
    """
    # Initialize with the new default settings
    client = PersistentClient(path=f"./persistent_dir_embeddings_{collection_name}")
    
    # Ensure the collection is created or retrieved
    collection = client.get_or_create_collection(name=collection_name, embedding_function=MyEmbeddingFunction())
    
    return client, collection

# Load or initialize metadata
def load_metadata(metadata_file):
    """
    Loads metadata from a predefined file if it exists.

    Returns:
        dict: The loaded metadata as a dictionary. Returns an empty dictionary if the file does not exist.

    Example:
        metadata = load_metadata()
    """
    if os.path.exists(metadata_file):
        with open(metadata_file, 'r') as f:
            return json.load(f)
    return {}

def save_metadata(metadata, metadata_file):
    """
    Saves the provided metadata to a predefined file.

    Parameters:
        metadata (dict): The metadata to be saved.

    Example:
        save_metadata(metadata)
    """
    with open(metadata_file, 'w') as f:
        json.dump(metadata, f, indent=4)

# Check if document embeddings already exist in Chroma
def check_document_in_chroma(pdf_hash, collection):
    """
    Checks if document embeddings already exist in Chroma for a given PDF hash.

    Parameters:
        pdf_hash (str): The hash of the PDF document.
        collection: The Chroma collection.

    Returns:
        bool: True if the document embeddings exist, False otherwise.

    Example:
        exists = check_document_in_chroma(pdf_hash, collection)
    """
    results = collection.get(
        where={"doc_hash": pdf_hash}, 
        include=["documents"]
    )
    
    # If documents matching the hash are found, return True
    return len(results['documents']) > 0

# Check if document embeddings already exist in Chroma
def check_document_in_chroma_metadata(pdf_hash, metadata):
    """
    Checks if document embeddings already exist in the metadata for a given PDF hash.

    Parameters:
        pdf_hash (str): The hash of the PDF document.
        metadata (dict): The metadata dictionary.

    Returns:
        bool: True if the document embeddings exist in the metadata, False otherwise.

    Example:
        exists = check_document_in_chroma_metadata(pdf_hash, metadata)
    """
    return pdf_hash in metadata

# Function to create embeddings for PDF chunks and store in Chroma
def create_embeddings_for_pdf(pdf_path, collection, metadata, metadata_file):
    """
    Creates embeddings for PDF chunks and stores them in Chroma.

    Parameters:
        pdf_path (str): The path to the PDF document.
        collection: The Chroma collection.
        metadata (dict): The metadata dictionary.

    Example:
        create_embeddings_for_pdf("example.pdf", collection, metadata)
    """
    pdf_hash = calculate_pdf_hash(pdf_path)
    
    # Check if document embeddings already exist
    # if check_document_in_chroma(pdf_hash, collection):
    #     print(f"Document '{pdf_path}' unchanged. Using existing embeddings.")
    #     return

    # Check if document is unchanged
    if check_document_in_chroma_metadata(pdf_hash, metadata):
        print(f"Document '{pdf_path}' unchanged. Skipping re-embedding.")
        return

    print(f"Processing new or updated document: {pdf_path}")
    
    # Load the PDF document
    # Extract text from the entire PDF
    full_text = extract_text_from_pdf(pdf_path)

    # Split the extracted text into chunks
    chunks = split_text_into_chunks(full_text, chunk_size=1000)  # You can adjust the chunk_size as needed

    #extract images
    images = extract_images_from_pdf(pdf_path)
    
    # Initialize embedding function (assuming you're using Ollama or any other embedding function)
    embF = MyEmbeddingFunction(batch_size=10)

    # Generate embeddings for all chunks in batches
    embeddings = embF.__call__(chunks)
    
    # Add documents to Chroma collection
    for i, embedding in enumerate(embeddings):
        if embedding is not None:
            collection.add(
                documents=[chunks[i]],
                metadatas=[{
                    'chunk_id': i,
                    'doc_hash': pdf_hash,
                    'source': pdf_path
                }],
                embeddings=[embedding],
                ids=[f"{pdf_path}_chunk{i}"]
            )
    """
    if images:
        #Generate Embeddings for Images:
        image_embeddings = embF.get_image_embedding(images)
        for i, embedding in enumerate(image_embeddings):
            if embedding is not None:
                collection.add(
                    documents=[f"Image {i} from {pdf_path}"],
                    metadatas=[{
                        'image_id': i,
                        'doc_hash': pdf_hash,
                        'source': pdf_path
                    }],
                    embeddings=[embedding],
                    ids=[f"{pdf_path}_image{i}"]
                )
    """      
    # Update metadata
    metadata[pdf_hash] = {'path': pdf_path}
    save_metadata(metadata, metadata_file)
    print(f"Embeddings for '{pdf_path}' created successfully.")

# Function to process header and source files
def process_reference_code(directory, collection, metadata_file):
    """
    Processes reference code files in a given directory by generating embeddings for their content
    and storing the embeddings in a specified collection.
    Args:
        directory (str): The root directory containing the 'inc' and 'src' subdirectories 
                         with header (.h) and source (.c) files respectively.
        collection (object): The collection object where embeddings will be stored. 
                             It should support the `add` method for adding documents, metadata, and embeddings.
        metadata_file (str): The path to the metadata file used to track processed files and their hashes.
    Workflow:
        1. Load metadata from the specified metadata file.
        2. Identify all header (.h) and source (.c) files in the 'inc' and 'src' subdirectories.
        3. Calculate a hash for each file to determine if it has been processed before.
        4. Skip processing for files that are unchanged based on their hash.
        5. For each unprocessed file:
            - Read the file content and split it into chunks.
            - Generate embeddings for each chunk using a custom embedding function.
            - Add the embeddings, along with metadata, to the specified collection.
        6. Update the metadata file with the hash and path of the processed file.
    Notes:
        - The function assumes the existence of helper functions such as `load_metadata`, 
          `calculate_pdf_hash`, `check_document_in_chroma_metadata`, `split_text_into_chunks`, 
          `MyEmbeddingFunction`, and `save_metadata`.
        - The `chunk_size` for splitting text and `batch_size` for embedding generation are hardcoded.
    Raises:
        FileNotFoundError: If the specified metadata file or any required subdirectory/file is not found.
        Exception: For any other errors encountered during file processing or embedding generation.
    Example:
        process_reference_code(
            directory="/path/to/codebase",
            collection=my_collection,
            metadata_file="/path/to/metadata.json"
    """
    metadata = load_metadata(metadata_file)

     # Remove entries for deleted PDFs
    remove_deleted_code_files_from_chroma(directory, collection, metadata, metadata_file)
    
    # List all header and source files in the directory
    header_files = [os.path.join(directory, f) for f in os.listdir(directory) if f.endswith('.h')]
    source_files = [os.path.join(directory, f) for f in os.listdir(directory) if f.endswith('.c')]
    
    # Combine all files to process
    all_files = header_files + source_files
    
    for file_path in all_files:
        # Calculate hash to check if the file has changed
        file_hash = calculate_pdf_hash(file_path)  # Reuse the hash function for consistency
        
        # Skip if already processed
        if check_document_in_chroma_metadata(file_hash, metadata):
            print(f"File '{file_path}' unchanged. Skipping re-embedding.")
            continue
        
        print(f"Processing file: {file_path}")
        
        # Read and split the file content into chunks
        with open(file_path, 'r', encoding='utf-8') as f:
            file_content = f.read()
        chunks = split_text_into_chunks(file_content, chunk_size=1000)
        
        # Generate embeddings for the chunks
        embF = MyEmbeddingFunction(batch_size=10)
        embeddings = embF.__call__(chunks)
        
        # Add embeddings to the collection
        for i, embedding in enumerate(embeddings):
            if embedding is not None:
                collection.add(
                    documents=[chunks[i]],
                    metadatas=[{
                        'chunk_id': i,
                        'doc_hash': file_hash,
                        'source': file_path
                    }],
                    embeddings=[embedding],
                    ids=[f"{file_path}_chunk{i}"]
                )
        
        # Update metadata
        metadata[file_hash] = {'path': file_path}
        save_metadata(metadata, metadata_file)
        print(f"Embeddings for '{file_path}' created successfully.")

# Remove embeddings for deleted PDFs
def remove_deleted_pdfs_from_chroma(directory, collection, metadata, metadata_file):
    """
    Remove embeddings for deleted PDFs from the collection.

    This function performs the following tasks:
    1. Identifies PDF files that have been deleted from the "docs" directory.
    2. Removes the corresponding entries from the collection and metadata.

    Parameters:
    collection (object): The collection object from which the PDF embeddings and metadata will be removed.
    metadata (dict): A dictionary containing metadata about the PDFs, where the key is the PDF hash and the value is a dictionary with PDF information.

    Returns:
    None

    Detailed Steps:
    1. Identify Existing Files: The function creates a set of existing PDF files in the "docs" directory.
    2. Identify Deleted Files: It then identifies which PDFs have been deleted by comparing the existing files with the metadata.
    3. Remove Deleted Files: For each deleted PDF, the function removes the corresponding entry from the collection and metadata, and prints a message indicating the removal.
    4. Save Updated Metadata: Finally, the function saves the updated metadata.

    Notes:
    - Ensure that the `save_metadata` function is defined and imported in the script.
    - The "docs" directory should contain only the PDF files that are currently in use.
    """
    existing_files = {f for f in os.listdir(directory) if f.endswith(".pdf")}
    hashes_to_remove = [
        pdf_hash for pdf_hash, info in metadata.items()
        if os.path.basename(info['path']) not in existing_files
    ]

    for pdf_hash in hashes_to_remove:
        print(f"Removing deleted document with hash: {pdf_hash}")
        collection.delete(where={"doc_hash": pdf_hash})
        del metadata[pdf_hash]
    
    save_metadata(metadata, metadata_file)

# Remove embeddings for deleted PDFs
def remove_deleted_code_files_from_chroma(directory, collection, metadata, metadata_file):
    """
    Remove embeddings for deleted PDFs from the collection.

    This function performs the following tasks:
    1. Identifies PDF files that have been deleted from the "docs" directory.
    2. Removes the corresponding entries from the collection and metadata.

    Parameters:
    collection (object): The collection object from which the PDF embeddings and metadata will be removed.
    metadata (dict): A dictionary containing metadata about the PDFs, where the key is the PDF hash and the value is a dictionary with PDF information.

    Returns:
    None

    Detailed Steps:
    1. Identify Existing Files: The function creates a set of existing PDF files in the "docs" directory.
    2. Identify Deleted Files: It then identifies which PDFs have been deleted by comparing the existing files with the metadata.
    3. Remove Deleted Files: For each deleted PDF, the function removes the corresponding entry from the collection and metadata, and prints a message indicating the removal.
    4. Save Updated Metadata: Finally, the function saves the updated metadata.

    Notes:
    - Ensure that the `save_metadata` function is defined and imported in the script.
    - The "docs" directory should contain only the PDF files that are currently in use.
    """
    # Identify existing .c and .h files
    existing_files = {f for f in os.listdir(directory) if f.endswith(('.c', '.h'))}
    
    # Identify hashes of .c and .h files that no longer exist in the directory
    hashes_to_remove = [
        file_hash for file_hash, info in metadata.items()
        if os.path.basename(info['path']) not in existing_files and info['path'].endswith(('.c', '.h'))
    ]

    # Remove deleted .c and .h files from the collection and metadata
    for file_hash in hashes_to_remove:
        print(f"Removing deleted code file with hash: {file_hash}")
        collection.delete(where={"doc_hash": file_hash})
        del metadata[file_hash]
    
    # Save the updated metadata
    save_metadata(metadata, metadata_file)
    
# Process all PDFs in the docs/ directory
def process_all_pdfs(directory, collection, metadata_file):
    """
    Process all PDFs in the specified directory.

    This function performs the following tasks:
    1. Loads metadata.
    2. Removes entries for deleted PDFs from a collection.
    3. Creates embeddings for each PDF file in the directory and updates the collection with the metadata.

    Parameters:
    directory (str): The path to the directory containing the PDF files to be processed.
    collection (object): The collection object where the PDF embeddings and metadata will be stored.

    Returns:
    None

    Example Usage:
    process_all_pdfs('docs/', my_collection)

    Detailed Steps:
    1. Load Metadata: The function starts by loading metadata using the `load_metadata` function.
    2. Remove Deleted PDFs: It then removes entries for deleted PDFs from the collection using the `remove_deleted_pdfs_from_chroma` function.
    3. Process PDF Files: The function lists all PDF files in the specified directory and iterates over each file. For each PDF file, it creates embeddings using the `create_embeddings_for_pdf` function and updates the collection with the metadata.

    Notes:
    - Ensure that the `load_metadata`, `remove_deleted_pdfs_from_chroma`, and `create_embeddings_for_pdf` functions are defined and imported in the script.
    - The `directory` should contain only PDF files that need to be processed.
    """
    metadata = load_metadata(metadata_file)
    
    # Remove entries for deleted PDFs
    remove_deleted_pdfs_from_chroma(directory, collection, metadata, metadata_file)

    pdf_files = [os.path.join(directory, f) for f in os.listdir(directory) if f.endswith('.pdf')]
    for pdf_path in pdf_files:
        create_embeddings_for_pdf(pdf_path, collection, metadata, metadata_file)

# Embedding user query and finding the best matching chunk
def find_relevant_chunk(user_query, collection):
    """
    Embed the user query and find the best matching chunk in the collection.

    This function performs the following tasks:
    1. Generates an embedding for the user query.
    2. Queries the collection to find the best matching chunks based on the query embedding.

    Parameters:
    user_query (str): The user's query string that needs to be embedded and matched.
    collection (object): The collection object where the embeddings and documents are stored.

    Returns:
    list or None: A list of documents that best match the user query, or None if no matches are found or if the embedding generation fails.

    Detailed Steps:
    1. Generate Query Embedding: The function uses `MyEmbeddingFunction` to generate an embedding for the user query.
    2. Check Embedding: It checks if the embedding generation was successful. If not, it prints an error message and returns None.
    3. Query Collection: The function queries the collection with the generated embedding to find the top 100 matching results.
    4. Return Results: If matching documents are found, it returns the list of documents. Otherwise, it returns None.

    Notes:
    - Ensure that the `MyEmbeddingFunction` class and its `__get_user_querry_embedding__` method are defined and imported in the script.
    - The `collection` object should support the `query` method with the specified parameters.
    """
    embF = MyEmbeddingFunction()
    query_embedding = embF.__get_user_querry_embedding__(user_query)
    
    # Check if embedding is None
    if query_embedding is None:
        print("Failed to generate embedding for the query.")
        return None
    
    results = collection.query(
        query_embeddings=[query_embedding],
        n_results=10
    )
    
    if results and 'documents' in results and len(results['documents']) > 0:
        return results['documents']
    return None

# Prompting the model for text generation
def prompt_model(messages, model: str = CHAT_MODEL, max_tokens: int = 5000, temperature: float = 0.5, top_p: float = 0.7, max_retries: int = 5, backoff_factor: int = 2) -> str:
    """
    Prompt the model for text generation.

    This function sends a prompt to a specified model and returns the generated text.

    Parameters:
    prompt_text (str): The text prompt to send to the model.
    model (str): The model to use for text generation. Default is CHAT_MODEL.
    max_tokens (int): The maximum number of tokens to generate. Default is 500.
    temperature (float): The sampling temperature. Default is 0.2.
    top_k (int): The number of highest probability vocabulary tokens to keep for top-k filtering. Default is 50.
    top_p (float): The cumulative probability for top-p (nucleus) sampling. Default is 0.7.

    Returns:
    str or None: The generated text if the request is successful and the response contains text, otherwise None.

    Detailed Steps:
    1. Prepare Request Data: The function prepares the data dictionary with the model parameters and prompt text.
    2. Send Request: It sends a POST request to the API endpoint for text generation.
    3. Check Response: The function checks if the response status is 200 (OK) and if the response contains generated text.
    4. Return Generated Text: If the response is valid and contains text, it returns the generated text. Otherwise, it returns None.

    Notes:
    - Ensure that the `requests` library is imported and the `API_URL` and `HEADERS` constants are defined in the script.
    - The model parameter should be a valid model identifier recognized by the API.
    """
    data = {
        'messages': messages,
        'max_tokens': max_tokens,
        'temperature': temperature,
        'top_p': top_p,
    }
    for attempt in range(max_retries):
        try:
            response = requests.post(f'{API_URL}/{CHAT_MODEL}/chat/completions?api-version={CHAT_API_VERSION}', headers=HEADERS, json=data)
            response.raise_for_status()  # Raises an HTTPError if the response was unsuccessful
            data_ = response.json()
            
            # Debugging: Print the response for troubleshooting
            # print("API Response:", data_)

            if 'choices' in data_ and len(data_['choices']) > 0:
                return data_['choices'][0]['message']['content']
            return None
        except requests.exceptions.RequestException as e:
            if isinstance(e, requests.exceptions.HTTPError) and e.response is not None and e.response.status_code == 429:
                # Handle rate limiting
                print(f"Rate limit exceeded. Retrying in {backoff_factor * (2 ** attempt)} seconds...")
                time.sleep(backoff_factor * (2 ** attempt))
            else:
                print(f"Request failed: {e}")
                return None
        except KeyError as e:
            print(f"KeyError in response: {e}")
            return None
    
def summarize_requirements(messages, collection):
    """
    Summarizes technical requirements based on provided messages and a collection of requirement chunks.
    Args:
        messages (list): A list of dictionaries representing messages. The first dictionary should contain 
                         a "content" key with the feature query as its value.
        collection (iterable): A collection of requirement chunks to search for relevant information.
    Returns:
        str or None: A summarized string of categorized requirements if relevant chunks are found, 
                     otherwise None.
    Raises:
        ValueError: If the feature query is not found in the provided messages.
    Functionality:
        1. Extracts the feature query from the first message in the `messages` list.
        2. Searches for relevant requirement chunks in the `collection` based on the feature query.
        3. If relevant chunks are found, constructs a prompt for an AI model to summarize the requirements.
        4. The AI model categorizes functional and non-functional requirements, highlights constraints, 
           dependencies, and assumptions, and ignores unrelated or ambiguous information.
    """
    # Extract the feature query from the first dictionary
    feature_query = messages[-1].get("content")

    if feature_query is None:
        raise ValueError("Feature query not found in messages.")
    
    relevant_chunks = find_relevant_chunk(feature_query, collection)
    if not relevant_chunks:
        print("No relevant requirements found.")
        return None
    
    # Define the system and user messages
    messages = [
        {
            "role": "system",
            "content": """
            You are a highly skilled AI assistant specializing in summarizing technical requirements. You are provided with Requirement Chunks delimited by tripple backticks.
            Your task is to:
            1. Identify and categorize all functional and non-functional requirements.
            2. Highlight any constraints, dependencies, or assumptions that may impact system design.
            3. Ignore unrelated or ambiguous information and ensure consistency.
            """
        },
        {
            "role": "user",
            "content": f"""
            Requirements:
            ```{relevant_chunks}```
            """
        }
    ]
    
    summary = prompt_model(messages)
    return summary

def extract_design_information(messages, collection):
    """
    Extracts design information based on summarized requirements and a reference design collection.
    This function processes a list of message dictionaries to extract summarized requirements 
    and a task description. It then identifies relevant reference design chunks from the provided 
    collection and uses these inputs to generate design information via a model prompt.
    Args:
        messages (list): A list of dictionaries containing message data. 
                         - The second dictionary should contain the summarized requirements under the "content" key.
                         - The third dictionary should contain the task description under the "content" key.
        collection (list): A collection of reference design information to search for relevant chunks.
    Returns:
        str or None: The generated design information as a string if successful, or None if no relevant 
                     design information is found.
    Raises:
        ValueError: If the summarized requirements are not found in the second dictionary of `messages`.
        ValueError: If the task description is not found in the third dictionary of `messages`.
    Notes:
        - The function assumes the input `messages` list contains at least three dictionaries.
        - The `prompt_model` function is used to generate the design information based on the system 
          and user messages.
    """
    requirements_summary = messages[1].get("content")
    if not requirements_summary:
        raise ValueError("Summarized requirements not found in the second dictionary of messages.")
    
    relevant_chunks = find_relevant_chunk(requirements_summary, collection)
    if not relevant_chunks:
        print("No relevant design information found.")
        return None
    
    # Extract the task from the third dictionary
    task = messages[-1].get("content")
    if not task:
        raise ValueError("Task not found in the third dictionary of messages.")

    # Define the system and user messages
    messages = [
        {
            "role": "system",
            "content": """
            You are a highly skilled AI assistant specializing in understanding Software Architechture. You are provided with Requirements and Information from Software Architechture delimited by tripple backticks.
            Your task is to:
            1. First understand the requirements and identify all the critical / important points mentioned in the requirements.
            2. Next, understand the extracted Software Architecture and identify relevant API functions, parameters, protocols, and constraints.
            3. Next Combine the understanding of the Software Architechture and the input Requirements into a unified view. If needed, bring out the delta information (or additional information) that is needed to realize the input Requirements.
            4. Make sure that the generated output addresses all the requirements and is consistent with the Software Architecture.
            5. Ignore unrelated or ambiguous information and ensure consistency.
            """
        },
        {
            "role": "user",
            "content": f"""
            Requirements:
            ```{requirements_summary}```
            Software Architecture:
            ```{relevant_chunks}```
            """
        }
    ]
    
    design_info = prompt_model(messages)
    return design_info

def extract_code_information(messages, collection):
    """
    Extract relevant code information from the reference code collection based on the requirements summary.

    Args:
        requirements_summary (str): The summarized requirements.
        reference_code_collection: The Chroma collection containing reference code embeddings.

    Returns:
        str: Relevant code information as a string, or None if no relevant code is found.
    """
    requirements_summary = messages[1].get("content")
    if not requirements_summary:
        raise ValueError("Summarized requirements not found in the second dictionary of messages.")
    
    relevant_chunks = find_relevant_chunk(requirements_summary, collection)
    if not relevant_chunks:
        print("No relevant code information found.")
        return None
    
    # Extract the task from the third dictionary
    task = messages[-1].get("content")
    if not task:
        raise ValueError("Task not found in the fourth dictionary of messages.")

    # Define the system and user messages
    messages = [
        {
            "role": "system",
            "content": """
            You are a highly skilled AI assistant specializing in understanding Software Designs from the Code. You are provided with Requirements and Code Information delimited by tripple backticks.
            Your task is to:
            1. First understand the requirements and identify all the critical / important points mentioned in the requirements.
            2. Next, understand the extracted Code Information and in your understanding include all relevant API functions, parameters, protocols, and constraints.
            3. Next Combine the understanding of the extracted Code Information and the input Requirements into a unified view. If needed, bring out the delta information (or additional information) that is needed to realize the input Requirements.
            4. Make sure that the generated output addresses all the requirements and is consistent with the extracted Code Information.
            5. Ignore unrelated or ambiguous information and ensure consistency.
            """
        },
        {
            "role": "user",
            "content": f"""
            Requirements:
            ```{requirements_summary}```
            Code Information:
            ```{relevant_chunks}```
            """
        }
    ]
    
    code_design_info = prompt_model(messages)
    return code_design_info

def create_uml_design(messages, uml_guidelines_collection):
    """
    Generates a UML design based on the provided messages and UML guidelines.
    This function extracts the UML diagram type and design information from the 
    `messages` parameter, retrieves relevant UML guidelines from the 
    `uml_guidelines_collection`, and constructs a prompt to generate the UML 
    design using a model. The generated UML design includes PlantUML code and 
    a detailed explanation.
    Args:
        messages (list): A list of dictionaries containing message data. 
            - The last dictionary in the list should contain the UML diagram type 
              in the "content" key.
            - The fourth dictionary should contain the design information in the 
              "content" key.
            - Optionally, the sixth dictionary may contain code design information 
              in the "content" key.
        uml_guidelines_collection (list): A collection of UML guidelines to extract 
            relevant guidelines for the requested UML diagram.
    Returns:
        str or None: The generated UML design as a string, including PlantUML code 
        and an explanation. Returns `None` if no UML guidelines are found.
    Raises:
        ValueError: If the design information is not found in the fourth dictionary 
        of the `messages` list.
    Notes:
        - The function dynamically adjusts the prompt based on the availability of 
          code design information in the `messages` list.
        - The `prompt_model` function is used to generate the UML design based on 
          the constructed prompt.
    """
    UML_Diagram = messages[-1].get("content")  # The last message contains the UML diagram type
    design_query = f"Extract the guidelines related to {UML_Diagram}"
    uml_guidelines = find_relevant_chunk(design_query, uml_guidelines_collection)
    if not uml_guidelines:
        print("No UML design guidelines found.")
        return None

    design_info = messages[3].get("content")
    if not design_info:
        raise ValueError("Design Information not found in the fourth dictionary of messages.")

    # Check if code design information is available
    Code_design_info = None
    if len(messages) > 5 and "content" in messages[5]:
        Code_design_info = messages[5].get("content")

    # Define system and user messages based on the availability of code design information
    if Code_design_info:
        # System message when code design information is available
        system_message = f"""
        You are a highly skilled AI assistant specializing in creating Software UML designs.
        Your task is to:
        1. Understand the provided Design Information and Code Design Information delimited by triple backticks.
        2. Create the requested ({UML_Diagram}) based on your understanding of the Design Information and Code Design Information.
        3. Make sure all the identified API Functions from the Design Information and Code Design Information are included in the UML Design.
        4. Provide PlantUML codes for the requested ({UML_Diagram}).
        5. Provide a detailed explanation of the requested PlantUML diagrams.
        """
        # User message when code design information is available
        user_message = f"""
        Design Information:
        ```{design_info}```
        Code Design Information:
        ```{Code_design_info}```
        UML Design Guidelines:
        ```{uml_guidelines}```
        """
    else:
        # System message when code design information is not available
        system_message = f"""
        You are a highly skilled AI assistant specializing in creating Software UML designs.
        Your task is to:
        1. Understand the provided Design Information delimited by triple backticks.
        2. Create the requested ({UML_Diagram}) based on your understanding of the Design Information.
        3. Make sure all the identified API Functions from the Design Information are included in the UML Design.
        4. Provide PlantUML codes for the requested ({UML_Diagram}).
        5. Provide a detailed explanation of the requested PlantUML diagrams.
        """
        # User message when code design information is not available
        user_message = f"""
        Design Information:
        ```{design_info}```
        UML Design Guidelines:
        ```{uml_guidelines}```
        """

    # Construct the messages for the prompt
    prompt_messages = [
        {"role": "system", "content": system_message},
        {"role": "user", "content": user_message}
    ]

    # Call the prompt_model function
    uml_design = prompt_model(prompt_messages)
    return uml_design


@app.errorhandler(Exception)
def handle_exception(e):
    """
    Handles exceptions by returning a JSON response with error details.
    Args:
        e (Exception): The exception object that was raised.
    Returns:
        tuple: A tuple containing a JSON response with the error details and 
               an HTTP status code of 500.
    """
    response = {
        "error": str(e),
        "message": "An error occurred while processing your request."
    }
    return jsonify(response), 500

@app.route('/embed_requirement_documents', methods=['POST'])
def embed_requirement_documents():
    """
    Embeds requirement documents into a Chroma collection.
    This function processes a list of file paths provided in a JSON payload,
    validates their existence, and embeds the content of the files into a 
    Chroma collection for further use. It uses a global variable `req_collection` 
    to store the collection reference.
    Returns:
        Response: A JSON response indicating success or failure of the operation.
    Raises:
        Exception: If any error occurs during the embedding process.
    JSON Payload:
        - Expected: A list of file paths (strings).
        - Example: ["path/to/file1.pdf", "path/to/file2.pdf"]
    Workflow:
        1. Parse the JSON payload from the request.
        2. Validate that the payload is a list of file paths.
        3. Initialize the Chroma client and collection.
        4. Process each file path:
            - Check if the file exists.
            - Embed the file content into the Chroma collection.
        5. Return a success message if all files are processed successfully.
        6. Handle and log any exceptions, returning an error response.
    Notes:
        - The collection name is set to "reqs" by default but can be adjusted.
        - Missing files are logged but do not interrupt the process.
    """
    global req_collection
    try:
        # Parse the JSON payload
        data = request.get_json()
        if not data or not isinstance(data, list):
            return jsonify({"error": "Invalid input. Expected a list of file paths."}), 400

        # Initialize Chroma client and metadata
        collection_name = "reqs"  # Adjust collection name as needed
        Reqclient, req_collection = init_chroma_client(collection_name)
        for pdf_path in data:
            if os.path.exists(pdf_path):
                process_all_pdfs(pdf_path, req_collection, REQUIREMENT_METADATA_FILE)
            else:
                print(f"File not found: {pdf_path}")

        return jsonify({"message": "Requirement Documents embedded successfully."}), 200
    except Exception as e:
        print(f"Error embedding documents: {e}")
        return jsonify({"error": str(e)}), 500
    
@app.route('/embed_reference_documents', methods=['POST'])
def embed_reference_documents():
    """
    Embeds reference documents into a Chroma collection for later use.
    This function processes a list of file paths provided in the JSON payload of an HTTP request.
    It initializes a Chroma client, validates the file paths, and processes each PDF file to embed
    its content into the specified Chroma collection.
    Global Variables:
        ref_collection: A global variable that holds the reference collection object.
    Returns:
        Response: A JSON response indicating the success or failure of the operation.
                  - On success: {"message": "Reference Documents embedded successfully."}, HTTP 200
                  - On failure: {"error": "Error message"}, HTTP 400 or 500
    Raises:
        Exception: If an error occurs during the embedding process, it is caught and logged.
    Notes:
        - The input JSON payload must be a list of file paths.
        - Each file path is validated to ensure the file exists before processing.
        - The function relies on external helper functions such as `init_chroma_client` and
          `process_all_pdfs`, as well as a metadata file defined by `REFERENCE_DATA_METADATA_FILE`.
    Example:
        Input JSON payload:
        [
            "/path/to/document1.pdf",
            "/path/to/document2.pdf"
        ]
        Response on success:
        {
            "message": "Reference Documents embedded successfully."
        }
    """
    global ref_collection
    try:
        # Parse the JSON payload
        data = request.get_json()
        if not data or not isinstance(data, list):
            return jsonify({"error": "Invalid input. Expected a list of file paths."}), 400

        # Initialize Chroma client and metadata
        collection_name = "refs"  # Adjust collection name as needed
        Refclient, ref_collection = init_chroma_client(collection_name)
        for pdf_path in data:
            if os.path.exists(pdf_path):
                process_all_pdfs(pdf_path, ref_collection, REFERENCE_DATA_METADATA_FILE)
            else:
                print(f"File not found: {pdf_path}")

        return jsonify({"message": "Reference Documents embedded successfully."}), 200
    except Exception as e:
        print(f"Error embedding documents: {e}")
        return jsonify({"error": str(e)}), 500
    
@app.route('/embed_code_documents', methods=['POST'])
def embed_code_documents():
    """
    Embeds source code documents into a Chroma collection for reference.
    This function processes a list of file paths provided in the JSON payload of an HTTP request.
    It initializes a Chroma client, validates the file paths, and processes each file to embed
    its content into a specified Chroma collection.
    Returns:
        Response: A JSON response indicating success or failure of the operation.
    Raises:
        Exception: If an error occurs during the embedding process.
    HTTP Request:
        - Input: JSON payload containing a list of file paths.
        - Output: JSON response with a success message or an error message.
    Notes:
        - The collection name is set to "code" by default but can be adjusted as needed.
        - Files that do not exist are skipped, and a message is printed to the console.
        - Errors during processing are logged and returned in the response.
    Example JSON Input:
        [
            "/path/to/file1.py",
            "/path/to/file2.py"
        ]
    Example JSON Response:
        Success: {"message": "Source Code embedded successfully."}
        Failure: {"error": "Error message describing the issue."}
    """
    global code_collection
    try:
        # Parse the JSON payload
        data = request.get_json()
        if not data or not isinstance(data, list):
            return jsonify({"error": "Invalid input. Expected a list of file paths."}), 400

        # Initialize Chroma client and metadata
        collection_name = "code"  # Adjust collection name as needed
        ReferenceCodeClient, code_collection = init_chroma_client(collection_name)
        for pdf_path in data:
            if os.path.exists(pdf_path):
                process_reference_code(pdf_path, code_collection, REFERENCE_CODE_METADATA_FILE)
            else:
                print(f"File not found: {pdf_path}")

        return jsonify({"message": "Source Code embedded successfully."}), 200
    except Exception as e:
        print(f"Error embedding documents: {e}")
        return jsonify({"error": str(e)}), 500
    
@app.route('/embed_guideline_documents', methods=['POST'])
def embed_guideline_documents():
    """
    Embeds guideline documents into a Chroma collection.
    This function processes a list of file paths provided in the JSON payload of an HTTP request.
    It initializes a Chroma client, validates the file paths, and embeds the content of the PDF
    files into a specified Chroma collection.
    Returns:
        Response: A JSON response indicating success or failure of the operation.
                  - On success: {"message": "Requirement Documents embedded successfully."}, HTTP 200
                  - On failure: {"error": "<error_message>"}, HTTP 400 or 500
    Raises:
        Exception: If an error occurs during the embedding process.
    Notes:
        - The JSON payload must contain a list of file paths.
        - The function checks if each file path exists before processing.
        - The collection name is set to "UML" by default but can be adjusted as needed.
    Global Variables:
        guideline_collection: A global variable to store the Chroma collection instance.
    Example JSON Payload:
        [
            "/path/to/document1.pdf",
            "/path/to/document2.pdf"
        ]
    """
    global guideline_collection
    try:
        # Parse the JSON payload
        data = request.get_json()
        if not data or not isinstance(data, list):
            return jsonify({"error": "Invalid input. Expected a list of file paths."}), 400

        # Initialize Chroma client and metadata
        collection_name = "UML"  # Adjust collection name as needed
        guidelineclient, guideline_collection = init_chroma_client(collection_name)
        for pdf_path in data:
            if os.path.exists(pdf_path):
                process_all_pdfs(pdf_path, guideline_collection, GUIDELINE_DATA_METADATA_FILE)
            else:
                print(f"File not found: {pdf_path}")

        return jsonify({"message": "UML guideline Documents embedded successfully."}), 200
    except Exception as e:
        print(f"Error embedding documents: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/summarize_requirements', methods=['POST'])
def summarize_requirements_api():
    """
    API endpoint to summarize requirements based on a feature query.
    This function handles a POST request containing a feature query and a session ID.
    It retrieves or initializes the session context, processes the feature query, and
    generates a summarized response of the requirements. The session context is updated
    with the user query and the assistant's response.
    Returns:
        Response: A JSON response containing the summarized requirements and the session ID.
                  If the requirements collection is not initialized, returns an error message
                  with a 400 status code.
    Request Body:
        - feature_query (str): The feature query provided by the user.
        - session_id (str): The session identifier for maintaining context.
    Response JSON:
        - requirements_summary (str): The summarized requirements generated by the assistant.
        - session_id (str): The session identifier for maintaining context.
    Error Response:
        - error (str): Error message indicating that the requirements collection is not initialized.
    """
    if req_collection is None:
        return jsonify({"error": "Requirements collection is not initialized. Please embed the Requirement documents first."}), 400
    data = request.json
    feature_query = data.get('feature_query', '')
    session_id = data.get('session_id', '')

    # Retrieve or initialize the session context
    if session_id not in chat_contexts:
        chat_contexts[session_id] = []
    
    messages = chat_contexts[session_id]

    # Add the feature query to the session context
    messages.append({"role": "user", "content": feature_query})

    # Generate the requirements summary
    requirements_summary = summarize_requirements(messages, req_collection)

    # Add the response to the session context
    messages.append({"role": "assistant", "content": requirements_summary})

    return jsonify({"requirements_summary": requirements_summary, "session_id": session_id})

@app.route('/extract_design_information', methods=['POST'])
def extract_design_information_api():
    """
    Extracts design information based on user input and a reference collection.
    This API endpoint processes a user's request to extract design-related 
    information by utilizing a session-based chat context and a reference 
    document collection. It validates the session ID, updates the chat context 
    with the user's input, and generates a response containing the extracted 
    design information.
    Returns:
        Response: A JSON response containing the extracted design information 
        and the session ID, or an error message with an appropriate HTTP status 
        code if the request is invalid.
    Error Responses:
        - 400: If the reference collection is not initialized.
        - 400: If the session ID is invalid or missing.
    JSON Input:
        {
            "session_id": str,  # The unique identifier for the user's session.
            "task_input": str   # The user's input or task description.
        }
    JSON Output:
        Success:
        {
            "design_info": str,  # The extracted design information.
            "session_id": str    # The session ID associated with the request.
        }
        Error:
        {
            "error": str  # Description of the error.
        }
    """
    if ref_collection is None:
        return jsonify({"error": "Reference collection is not initialized. Please embed the Reference documents first."}), 400
    data = request.json
    session_id = data.get('session_id', '')
    task_input = data.get('task_input', '')

    if not session_id or session_id not in chat_contexts:
        return jsonify({"error": "Invalid or missing session ID."}), 400
    
    # Retrieve the chat context for the session
    messages = chat_contexts[session_id]

    # Add the new user message to the context
    messages.append({"role": "user", "content": task_input})

    design_info = extract_design_information(messages, ref_collection)

    # Add the response to the session context
    messages.append({"role": "assistant", "content": design_info})

    return jsonify({"design_info": design_info, "session_id": session_id})

@app.route('/extract_code_information', methods=['POST'])
def extract_code_information_api():
    """
    Handles an API request to extract code design information based on user input and session context.
    This function processes a JSON request containing a session ID and task input, validates the session,
    retrieves the chat context for the session, and uses the provided input to extract code design information.
    The extracted information is then added to the session context and returned as a JSON response.
    Returns:
        Response: A Flask JSON response containing the extracted code design information and session ID,
                  or an error message with an appropriate HTTP status code if the request is invalid.
    Raises:
        KeyError: If the session ID is not found in the chat contexts.
        TypeError: If the input data is not in the expected format.
    JSON Request Parameters:
        - session_id (str): The unique identifier for the user's session.
        - task_input (str): The user's input describing the task or code to analyze.
    JSON Response:
        - code_design_info (str): The extracted code design information.
        - session_id (str): The session ID associated with the request.
    Error Responses:
        - 400 Bad Request: If the session ID is invalid or missing, or if the code collection is not initialized.
    """
    if code_collection is None:
        return jsonify({"error": "Code collection is not initialized. Please embed the source first."}), 400
    data = request.json
    session_id = data.get('session_id', '')
    task_input = data.get('task_input', '')

    if not session_id or session_id not in chat_contexts:
        return jsonify({"error": "Invalid or missing session ID."}), 400
    
    # Retrieve the chat context for the session
    messages = chat_contexts[session_id]

    # Add the new user message to the context
    messages.append({"role": "user", "content": task_input})

    code_design_info = extract_code_information(messages, code_collection)

    # Add the response to the session context
    messages.append({"role": "assistant", "content": code_design_info})

    return jsonify({"code_design_info": code_design_info, "session_id": session_id})

@app.route('/create_uml_design', methods=['POST'])
def create_uml_design_api():
    """
    Handles the creation of a UML design based on user input and session context.
    This API endpoint processes a request to generate a UML design by utilizing
    a chat-based context and a pre-initialized guideline collection. It validates
    the session ID, retrieves the chat context, and appends the user's input to
    the session messages. The UML design is then generated and added to the session
    context before being returned in the response.
    Returns:
        Response: A JSON response containing the generated UML design and the session ID.
                  If an error occurs, an appropriate error message and status code are returned.
    Raises:
        400 Bad Request: If the guideline collection is not initialized, the session ID is
                         invalid or missing, or other required data is not provided.
    Request JSON Structure:
        {
            "session_id": str,  # The unique identifier for the session.
            "task_input": str   # The UML diagram input provided by the user.
        }
    Response JSON Structure:
        Success:
        {
            "uml_design": str,  # The generated UML design.
            "session_id": str   # The session ID associated with the request.
        }
        Error:
        {
            "error": str  # Description of the error.
        }
    """
    if guideline_collection is None:
        return jsonify({"error": "Guideline collection is not initialized. Please embed the Guideline documents first."}), 400
    data = request.json
    session_id = data.get('session_id', '')

    if not session_id or session_id not in chat_contexts:
        return jsonify({"error": "Invalid or missing session ID."}), 400

    # Retrieve the chat context for the session
    messages = chat_contexts[session_id]

    # design_info = data.get('design_info', '')
    # code_design_info = data.get('code_design_info', '')
    UML_Diagram = data.get('task_input', '')

    # Add the new user message to the context
    messages.append({"role": "user", "content": UML_Diagram})

    uml_design = create_uml_design(messages, guideline_collection)

    # Add the response to the session context
    messages.append({"role": "assistant", "content": uml_design})

    return jsonify({"uml_design": uml_design, "session_id": session_id})

if __name__ == "__main__":
    # Start Flask server
    app.run(host='0.0.0.0', port=5000)