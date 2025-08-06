import unittest
import os
from src.main.app import gemini_service


class RealGeminiIntegrationTest(unittest.IsolatedAsyncioTestCase):

    async def test_generate_real_response(self):
        prompt_path = os.path.join("src", "main", "static", "prompt", "prompt_template.txt")
        prompt_template = gemini_service.load_prompt_template(prompt_path)

        question = "Вопросы с одним правильным вариантом: 10, Вопросы с множественным выбором: 5"
        user_prompt = gemini_service.create_user_prompt(prompt_template, question)

        file_path = os.path.join("src", "tests", "resources", "test.pdf")
        with open(file_path, "rb") as f:
            file_bytes = f.read()

        uploaded_files = await gemini_service.upload_files_to_gemini([file_bytes])

        result_text = gemini_service.generate_gemini_response(uploaded_files, user_prompt)

        print("\n🔍 Ответ от Gemini:\n", result_text)

        self.assertTrue(len(result_text.strip()) > 0, "Ответ от Gemini пустой!")