from typing import List
from io import BytesIO
from google import genai
from google.genai import types
from src.main.app.config import GEMINI_API_KEY

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
        uploaded_file = client.files.upload(
            file=file_stream,
            config=types.UploadFileConfig(mime_type='application/pdf')
        )
        uploaded_files.append(uploaded_file)
    return uploaded_files


def generate_gemini_response(uploaded_files, user_prompt: str) -> str:
    """Формирует и отправляет запрос к Gemini, возвращает ответ как строку."""
    parts = []
    for file in uploaded_files:
        file_data = types.FileData(file_uri=file.uri)
        parts.append(types.Part(file_data=file_data))

    parts.append(types.Part(text=user_prompt))

    contents = types.Content(parts=parts)

    response = client.models.generate_content(
        model="gemini-2.5-flash",
        contents=contents
    )
    return response.text