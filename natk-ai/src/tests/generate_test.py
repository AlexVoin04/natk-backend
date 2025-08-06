import unittest
from fastapi.testclient import TestClient
from unittest.mock import patch, AsyncMock, MagicMock
from src.main.app.main import app
import os

class ProcessFileTestCase(unittest.TestCase):

    @patch("src.main.app.gemini_service.load_prompt_template")
    @patch("src.main.app.gemini_service.create_user_prompt")
    @patch("src.main.app.gemini_service.upload_files_to_gemini", new_callable=AsyncMock)
    @patch("src.main.app.gemini_service.generate_gemini_response")
    def test_process_file_success(
        self,
        mock_generate_response,
        mock_upload_files,
        mock_create_prompt,
        mock_load_template
    ):
        mock_load_template.return_value = "Answer this question: {question}"
        mock_create_prompt.return_value = "Answer this question: Вопросы с одним правильным вариантом: 10, Вопросы с множественным выбором: 5"
        mock_upload_files.return_value = ["mock_file"]
        mock_generate_response.return_value = "AI is artificial intelligence."

        client = TestClient(app)

        file_path = os.path.join("src","tests", "resources", "test.pdf")
        with open(file_path, "rb") as f:
            files = [
                ("files", ("test.pdf", f.read(), "application/pdf"))
            ]

        data = {"question": "What is AI?"}

        response = client.post("/process-file/", data=data, files=files)

        self.assertEqual(response.status_code, 200)
        self.assertIn("result", response.json())
        self.assertEqual(response.json()["result"], "AI is artificial intelligence.")

if __name__ == '__main__':
    unittest.main()
