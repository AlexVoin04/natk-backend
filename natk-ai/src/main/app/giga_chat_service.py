from gigachat import GigaChat
from gigachat.models import AccessToken, Chat, Messages, MessagesRole
from typing import List, Optional
from enum import Enum
from io import BytesIO

from src.main.app.config import GIGA_CHAT_API_KEY
from src.main.app.exceptions import GigaChatServiceError

class GigaModel(str, Enum):
    BASE = "GigaChat"
    PRO = "GigaChat-Pro"
    MAX = "GigaChat-Max"

def get_giga_client(model: Optional[GigaModel] = None) -> GigaChat:
    """Создаёт и возвращает клиента GigaChat."""
    return GigaChat(
        credentials=GIGA_CHAT_API_KEY,
        verify_ssl_certs=False,
        model=model.value if model else None
    )

def get_auth_token() -> AccessToken:
    """Получение токена авторизации GigaChat."""
    giga = get_giga_client()
    try:
        response = giga.get_token()
        return response
    except Exception as e:
        raise GigaChatServiceError(f"Не удалось получить токен GigaChat. Ошибка: {str(e)}") from None

def upload_files_to_giga(files: List[bytes], filenames: Optional[List[str]] = None) -> List[str]:
    """
    Загружает файлы в GigaChat и возвращает список их ID.
    :param files: список содержимого файлов в байтах.
    :param filenames: список имён файлов, если не указано — будут test_N.pdf
    """
    giga = get_giga_client()
    uploaded_files = []

    for idx, content in enumerate(files):
        name = filenames[idx] if filenames and idx < len(filenames) else f"test_{idx + 1}.pdf"
        try:
            with BytesIO(content) as file_stream:
                file = giga.upload_file((name, file_stream, "application/pdf"))
                uploaded_files.append(file.id_)
        except Exception as e:
            raise GigaChatServiceError(f"Не удалось загрузить файл: {name}. Ошибка: {str(e)}") from None

    return uploaded_files

def generate_giga_response(
        uploaded_files: List[str],
        system_prompt: str,
        user_message: str,
        model: GigaModel = GigaModel.BASE
) -> str:
    """
    Генерирует ответ от GigaChat с прикреплёнными файлами.
    """
    giga = get_giga_client(model)

    payload = Chat(
        messages=[
            Messages(
                role=MessagesRole.SYSTEM,
                content= system_prompt,
            ),
            Messages(
                role=MessagesRole.USER,
                content=user_message,
                attachments=uploaded_files,
            ),
        ],
        temperature=0.1
    )

    try:
        result = giga.chat(payload)
        return result.choices[0].message.content
    except Exception as e:
        raise GigaChatServiceError(f"Ошибка генерации ответа от GigaChat. Ошибка: {str(e)}") from None