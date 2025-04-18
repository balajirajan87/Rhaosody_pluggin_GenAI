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

# Constants for Embedding and LLM API
TEXT_EMBEDDING_MODEL = "text-embedding-3-small"
IMAGE_EMBEDDING_MODEL = "image-embedding-1"
CHAT_MODEL = "gpt-4o"
API_URL = 'https://api.openai.com/v1'
API_KEY = os.getenv('OPENAI_API_KEY')  # Read API key from environment variable

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
            'model': TEXT_EMBEDDING_MODEL,
            'input': batch_data,  # Sending a list of inputs for batch processing
            'dimensions': 1024
        }
        
        for attempt in range(self.max_retries):
            try:
                response = requests.post(f'{API_URL}/embeddings', headers=HEADERS, json=data)
                response.raise_for_status()  # Raises an HTTPError if the response was unsuccessful
                data_ = response.json()
                
                # Debugging: Print the response for troubleshooting
                print("API Response:", data_)

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
        image_data = [image.tobytes() for image in images]
        data = {
            'model': IMAGE_EMBEDDING_MODEL,
            'input': image_data,
            'dimensions': 1024
        }
        for attempt in range(self.max_retries):
            try:
                response = requests.post(f'{API_URL}/embeddings', headers=HEADERS, json=data)
                response.raise_for_status()
                data_ = response.json()
                if 'data' in data_:
                    embeddings = [item.get('embedding') for item in data_['data']]
                    return embeddings
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
            'model': TEXT_EMBEDDING_MODEL,
            'input': user_querry,
            'dimensions': 1024
        }
        
        for attempt in range(self.max_retries):
            try:
                response = requests.post(f'{API_URL}/embeddings', headers=HEADERS, json=data)
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
    metadata = load_metadata(metadata_file)
    
    # Define subdirectories for header and source files
    inc_dir = os.path.join(directory, "inc")
    src_dir = os.path.join(directory, "src")
    
    # List all header and source files
    header_files = [os.path.join(inc_dir, f) for f in os.listdir(inc_dir) if f.endswith('.h')]
    source_files = [os.path.join(src_dir, f) for f in os.listdir(src_dir) if f.endswith('.c')]
    
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
    hashes_to_remove = [pdf_hash for pdf_hash, info in metadata.items() if info['path'].split('/')[-1] not in existing_files]

    for pdf_hash in hashes_to_remove:
        print(f"Removing deleted document with hash: {pdf_hash}")
        collection.delete(where={"doc_hash": pdf_hash})
        del metadata[pdf_hash]
    
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
def prompt_model(messages, model: str = CHAT_MODEL, max_tokens: int = 1000, temperature: float = 0.2, top_p: float = 0.7, max_retries: int = 5, backoff_factor: int = 2) -> str:
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
        'model': model,
        'messages': messages,
        'max_tokens': max_tokens,
        'temperature': temperature,
        'top_p': top_p,
    }
    for attempt in range(max_retries):
        try:
            response = requests.post(f'{API_URL}/chat/completions', headers=HEADERS, json=data)
            response.raise_for_status()  # Raises an HTTPError if the response was unsuccessful
            data_ = response.json()
            
            # Debugging: Print the response for troubleshooting
            print("API Response:", data_)

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
    # Extract the feature query from the first dictionary
    feature_query = messages[0].get("content")

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
    requirements_summary = messages[1].get("content")
    if not requirements_summary:
        raise ValueError("Summarized requirements not found in the second dictionary of messages.")
    
    relevant_chunks = find_relevant_chunk(requirements_summary, collection)
    if not relevant_chunks:
        print("No relevant design information found.")
        return None
    
    # Extract the task from the third dictionary
    task = messages[2].get("content")
    if not task:
        raise ValueError("Task not found in the third dictionary of messages.")

    # Define the system and user messages
    messages = [
        {
            "role": "system",
            "content": f"""
            You are a highly skilled AI assistant specializing in understanding software architechture / reference Design informations. You are provided with Requirements and Reference Design Information delimited by tripple backticks and task delimited by Angle brackets.
            Your task is:
            <{task}>
            """
        },
        {
            "role": "user",
            "content": f"""
            Requirements:
            ```{requirements_summary}```
            Reference Design Information:
            ```{relevant_chunks}```
            """
        }
    ]
    
    design_info = prompt_model(messages)
    return design_info

def extract_code_information(requirements_summary, reference_code_collection):
    """
    Extract relevant code information from the reference code collection based on the requirements summary.

    Args:
        requirements_summary (str): The summarized requirements.
        reference_code_collection: The Chroma collection containing reference code embeddings.

    Returns:
        str: Relevant code information as a string, or None if no relevant code is found.
    """
    relevant_chunks = find_relevant_chunk(requirements_summary, reference_code_collection)
    if not relevant_chunks:
        print("No relevant code information found.")
        return None

    # Define the system and user messages
    messages = [
        {
            "role": "system",
            "content": """
            You are a highly skilled AI assistant specializing in understanding Code information. You are provided with Requirements and its associated Code Information delimited by tripple backticks.
            Your task is to:
            1. Identify relevant API functions, parameters, protocols, and constraints from the Code Information.
            2. Bring out an understanding of the Code information and map them with the requirements.
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
    UML_Diagram = messages[4].get("content")
    design_querry = f"Extract the guidelines related to {UML_Diagram}"
    uml_guidelines = find_relevant_chunk(design_querry, uml_guidelines_collection)
    if not uml_guidelines:
        print("No UML design guidelines found.")
        return None
    
    design_info = messages[3].get("content")
    if not design_info:
        raise ValueError("Design Information not found in the third dictionary of messages.")

    # Define the system and user messages
    messages = [
        {
            "role": "system",
            "content": f"""
            You are a highly skilled AI assistant specializing in creating Software UML designs.
            Your task is to:
            1. Understand the provided Design Information delimited by tripple backticks.
            2. Create the requested ({UML_Diagram}) based on your understanding of the Design information.
            3. Make sure all the identified API Functions, from the Design Information are included in the UML Design.
            4. Provide PlantUML codes for the requested ({UML_Diagram}).
            5. Provide a detailed explanation of the requested PlantUML diagrams.
            """
        },
        {
            "role": "user",
            "content": f"""
            Design Information:
            ```{design_info}```
            UML Design Guidelines:
            ```{uml_guidelines}```
            """
        }
    ]
    
    uml_design = prompt_model(messages)
    return uml_design


@app.errorhandler(Exception)
def handle_exception(e):
    response = {
        "error": str(e),
        "message": "An error occurred while processing your request."
    }
    return jsonify(response), 500

@app.route('/embed_requirement_documents', methods=['POST'])
def embed_requirement_documents():
    """
    API endpoint to receive Requirement document paths and embed them.
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
    API endpoint to receive Reference Data document paths and embed them.
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
    API endpoint to receive Reference Data document paths and embed them.
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
    API endpoint to receive Design Guidelines Data document paths and embed them.
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

        return jsonify({"message": "Requirement Documents embedded successfully."}), 200
    except Exception as e:
        print(f"Error embedding documents: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/summarize_requirements', methods=['POST'])
def summarize_requirements_api():
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