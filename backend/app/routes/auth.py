from fastapi import APIRouter, HTTPException, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel
import json
import bcrypt
from pathlib import Path
import os
from datetime import datetime, timedelta
from jose import jwt, JWTError

router = APIRouter(prefix="/authority", tags=["Authority Auth"])

# ---------- Request schema ----------
class AuthorityLoginRequest(BaseModel):
    authority_id: str
    password: str

# ---------- Load authorities ----------
BASE_DIR = Path(__file__).resolve().parents[2]
AUTH_FILE = BASE_DIR / "authorities.json"

def load_authorities():
    if not AUTH_FILE.exists():
        raise RuntimeError("authorities.json not found")

    with open(AUTH_FILE, "r") as f:
        data = json.load(f)

    return data.get("authorities", [])

# ---------- JWT config ----------
JWT_SECRET_KEY = os.getenv("JWT_SECRET_KEY")
JWT_ALGORITHM = os.getenv("JWT_ALGORITHM", "HS256")
JWT_EXPIRE_MINUTES = int(os.getenv("JWT_EXPIRE_MINUTES", 60))

def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=JWT_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, JWT_SECRET_KEY, algorithm=JWT_ALGORITHM)

# ---------- Auth dependency ----------
security = HTTPBearer()

def get_current_authority(
    credentials: HTTPAuthorizationCredentials = Depends(security)
):
    token = credentials.credentials

    try:
        payload = jwt.decode(token, JWT_SECRET_KEY, algorithms=[JWT_ALGORITHM])

        authority_id = payload.get("sub")
        role = payload.get("role")

        if authority_id is None or role != "authority":
            raise HTTPException(status_code=401, detail="Invalid token")

        return authority_id

    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid or expired token")

# ---------- Login API ----------
@router.post("/login")
def authority_login(payload: AuthorityLoginRequest):
    authorities = load_authorities()

    for authority in authorities:
        if authority["authority_id"].strip().lower() == payload.authority_id.strip().lower():
            stored_hash = authority["password_hash"].encode()

            if bcrypt.checkpw(payload.password.encode(), stored_hash):
                access_token = create_access_token({
                    "sub": authority["authority_id"],
                    "role": authority.get("role", "authority")
                })

                return {
                    "access_token": access_token,
                    "token_type": "bearer"
                }

            break

    raise HTTPException(status_code=401, detail="Invalid authority credentials")
