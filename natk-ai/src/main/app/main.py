from fastapi import FastAPI, File, UploadFile, Form, HTTPException
from fastapi.responses import JSONResponse
from typing import List
from dotenv import load_dotenv

from src.main.app import gemini_service
from src.main.app.exceptions import GeminiAPIError

app = FastAPI()
load_dotenv()

@app.post("/generate-questions/")
async def generate_questions(
    question: str = Form(...),
    files: List[UploadFile] = File(...)
):
    try:
        prompt_template = gemini_service.load_prompt_template("src/main/static/prompt/prompt_template.txt")
        user_prompt = gemini_service.create_user_prompt(prompt_template, question)

        file_contents = [await f.read() for f in files]
        uploaded_files = await gemini_service.upload_files_to_gemini(file_contents)

        result_text = gemini_service.generate_gemini_response(uploaded_files, user_prompt)

        return JSONResponse(content={"result": result_text})

    except GeminiAPIError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail="Internal server error: "+ str(e))