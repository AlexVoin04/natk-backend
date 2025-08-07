class GeminiAPIError(Exception):
    """Базовая ошибка при работе с Gemini API."""
    pass

class GeminiFileUploadError(GeminiAPIError):
    """Ошибка при загрузке файла."""
    pass

class GeminiGenerationError(GeminiAPIError):
    """Ошибка при генерации ответа."""
    pass