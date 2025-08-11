from fastapi import FastAPI, File, UploadFile, Form, HTTPException
from fastapi.responses import JSONResponse
from typing import List
from dotenv import load_dotenv
import os

from src.main.app import gemini_service
from src.main.app import giga_chat_service
from src.main.app.exceptions import GeminiAPIError, GigaChatServiceError

app = FastAPI()
load_dotenv()

@app.post("/generate-questions/")
async def generate_questions(
    provider: str = Form(...),  # "gemini" или "giga"
    question: str = Form(...),
    files: List[UploadFile] = File(...)
):
    try:
        prompt_template = gemini_service.load_prompt_template(
            "src/main/static/prompt/prompt_template.txt"
        )

        file_contents = [await f.read() for f in files]

        if provider.lower() == "gemini":
            # --- Gemini ---
            user_prompt = gemini_service.create_user_prompt(prompt_template, question)
            uploaded_files = await gemini_service.upload_files_to_gemini(file_contents)
            result_text = gemini_service.generate_gemini_response(uploaded_files, user_prompt)

        elif provider.lower() == "giga":
            # --- GigaChat ---
            uploaded_file_ids = giga_chat_service.upload_files_to_giga(file_contents)
            result_text = giga_chat_service.generate_giga_response(
                uploaded_file_ids,
                prompt_template,
                question,
                giga_chat_service.GigaModel.MAX
            )
        else:
            raise HTTPException(status_code=400, detail="Invalid provider value. Use 'gemini' or 'giga'.")

        return JSONResponse(content={"result": result_text})

    except GeminiAPIError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except GigaChatServiceError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail="Internal server error: "+ str(e))