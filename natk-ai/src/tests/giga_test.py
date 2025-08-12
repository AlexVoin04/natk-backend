import os
import unittest

from src.main.app import giga_chat_service
from src.main.app import gemini_service


class RealGigaIntegrationTest(unittest.IsolatedAsyncioTestCase):

    def test_generate_real_response(self):
        prompt_path = os.path.join("src", "main", "static", "prompt", "prompt_template_ru.txt")
        prompt_template = gemini_service.load_prompt_template(prompt_path)

        question = (
            "Сгенерируй тест из следующих вопросов: "
            "CHOICE: 5, MULTIPLE_CHOICE: 5, SHORT_ANSWER: 0, "
            "TRUE_FALSE: 2, COMPLIANCE: 3, ESSAY: 0"
        )

        file_path = os.path.join("src", "tests", "resources", "test.pdf")
        with open(file_path, "rb") as f:
            file_bytes = f.read()

        uploaded_files = giga_chat_service.upload_files_to_giga([file_bytes])

        result_text = giga_chat_service.generate_giga_response(
            uploaded_files,
            prompt_template,
            question,
            giga_chat_service.GigaModel.BASE
        )

        print("\n🔍 Ответ от Giga:\n", result_text)

        self.assertTrue(len(result_text.strip()) > 0, "Ответ от Giga пустой!")
