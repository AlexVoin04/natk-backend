from typing import List
from io import BytesIO
from google import genai
from google.genai import types
from google.genai.errors import ClientError

from src.main.app.config import GEMINI_API_KEY
from src.main.app.exceptions import GeminiFileUploadError, GeminiGenerationError

client = genai.Client(api_key=GEMINI_API_KEY)

def load_prompt_template(filepath: str) -> str:
    """Читает и возвращает шаблон промта из файла."""
    with open(filepath, "r", encoding="utf-8") as f:
        return f.read()


def create_user_prompt(prompt_template: str, question: str) -> str:
    """Генерирует итоговый промт с подстановкой вопроса."""
    return prompt_template.replace("{question}", question)


async def upload_files_to_gemini(files: List[bytes]) -> List[types.File]:
    """Загружает список байтов файлов в Gemini API и возвращает объекты загруженных файлов."""
    uploaded_files = []
    for content in files:
        file_stream = BytesIO(content)
        try:
            uploaded_file = client.files.upload(
                file=file_stream,
                config=types.UploadFileConfig(mime_type='application/pdf')
            )
            uploaded_files.append(uploaded_file)
        except ClientError as e:
            message = extract_client_error_message(e, str(e))
            raise GeminiFileUploadError(f"Error when uploading a file: {message}")
        except Exception as e:
            raise GeminiFileUploadError(f"Unknown error when uploading a file: {str(e)}")
    return uploaded_files


def generate_gemini_response(uploaded_files, user_prompt: str) -> str:
    """Формирует и отправляет запрос к Gemini, возвращает ответ как строку."""
    try:
        parts = [types.Part(file_data=types.FileData(file_uri=f.uri)) for f in uploaded_files]
        parts.append(types.Part(text=user_prompt))
        contents = types.Content(parts=parts)

        response = client.models.generate_content(
            model="gemini-2.5-flash",
            contents=contents
        )
        return response.text

    except ClientError as e:
        message = extract_client_error_message(e, str(e))
        raise GeminiGenerationError(f"Error generating the response: {message}")
    except Exception as e:
        raise GeminiGenerationError(f"Unknown generation error: {str(e)}")

def extract_client_error_message(e: ClientError, default_message: str = None) -> str:
    """Извлекает сообщение об ошибке из ClientError."""
    try:
        error_json = e.response.json() if hasattr(e.response, 'json') else None
        if isinstance(error_json, dict):
            return error_json.get("error", {}).get("message", default_message or str(e))
    except (AttributeError, TypeError, ValueError):
        pass
    return default_message or str(e)