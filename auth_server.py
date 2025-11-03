import httpx
import time
import hmac
import hashlib
import secrets
from fastapi import FastAPI, Request, Header, HTTPException, status
from pydantic import BaseModel

app = FastAPI()


# --- Pydantic Models for the request body ---
class User(BaseModel):
    username: str
    password: str

class SignedRequest(BaseModel):
    question: str
    token: str


MINECRAFT_RAG_SERVER_URL = (
    "https://minecraft-rag-server-592661633041.us-central1.run.app"
)
# IMPORTANT: Replace with your actual API key from the minecraft-rag-server environment
RAG_SERVER_CHATBOT_API_KEY = "52398200ee29bbb2bfeff409f8a57a71"

# --- Security ---
# This is a simple in-memory user database.
# In a production environment, use a proper database.
users = {}

# This is a simple in-memory token database.
# In a production environment, use a proper database or a token-based authentication system like JWT.
sessions = {}


@app.post("/signup")
async def signup(user: User):
    if user.username in users:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Username already exists",
        )
    users[user.username] = hashlib.sha256(user.password.encode()).hexdigest()
    return {"message": "User created successfully"}


@app.post("/signin")
async def signin(user: User):
    if user.username not in users:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid username or password",
        )
    if users[user.username] != hashlib.sha256(user.password.encode()).hexdigest():
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid username or password",
        )
    token = secrets.token_urlsafe(32)
    sessions[token] = user.username
    return {"token": token}


async def get_rag_token(client: httpx.AsyncClient):
    """Fetches a new temporary token from the Minecraft-RAG server."""
    try:
        response = await client.post(
            f"{MINECRAFT_RAG_SERVER_URL}/get_token",
            headers={"X-API-Key": RAG_SERVER_CHATBOT_API_KEY},
        )
        response.raise_for_status()
        token_data = response.json()
        return token_data.get("token")
    except httpx.HTTPStatusError as exc:
        raise HTTPException(
            status_code=exc.response.status_code,
            detail=f"Error getting RAG server token: {exc.response.text}",
        )
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to obtain RAG server token: {str(exc)}",
        )


@app.post("/get-data")
async def get_data(signed_request: SignedRequest):
    """
    This endpoint verifies the signed request, obtains a temporary token,
    forwards the question to the Minecraft-RAG server, and returns the response.
    """
    if signed_request.token not in sessions:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
        )

    async with httpx.AsyncClient(timeout=20.0) as client:
        # 2. Get a valid token for the Minecraft-RAG Server
        rag_token = await get_rag_token(client)
        if not rag_token:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Could not obtain token for RAG server",
            )

        # 3. Forward the question to the Minecraft-RAG Server
        try:
            response = await client.post(
                f"{MINECRAFT_RAG_SERVER_URL}/ask",
                headers={"Authorization": f"Bearer {rag_token}"},
                json={"question": signed_request.question},
            )
            response.raise_for_status()

            # 4. Return the response from the Minecraft-RAG Server
            return response.json()

        except httpx.HTTPStatusError as exc:
            raise HTTPException(
                status_code=exc.response.status_code,
                detail=f"Error from Minecraft-RAG server: {exc.response.text}",
            )
        except Exception as exc:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"An internal server error occurred: {str(exc)}",
            )


@app.get("/")
def read_root():
    return {"message": "Auth Server is running and ready to connect to RAG server."}
