# TruthGuard AI — Python Backend Service

A high-performance, modular fact-checking, rumor detection, and scam analysis backend.

## Features
- **Semantic Claim Verification (`/api/v1/verify`)**: Cross-checks claims against documented cybercrime scams (Digital Arrest, fake government schemes, task scams, electricity cutoffs, banknote rumors) and Gemini 3.5 Flash intelligence.
- **SSRF-Protected URL Inspector (`/api/v1/inspect-url`)**: Protects against Server-Side Request Forgery (SSRF), blocks private/loopback/link-local IP addresses, flags suspicious TLDs (`.xyz`, `.top`, `.click`), punycode, and brand impersonation.
- **GuardBot AI Assistant (`/api/v1/chat`)**: Provides cybersecurity advice, scam explanations, and national helpline guidance (1930).
- **Dual Execution Engine**:
  - **FastAPI + Uvicorn** when modern web packages are available (automatic Swagger documentation at `/docs`).
  - **Standard Library Server** fallback (`http.server`) requiring zero external dependencies.

## Quick Start Commands

### 1. Run with FastAPI & Uvicorn (Recommended for production):
```bash
cd backend
pip install -r requirements.txt
export GEMINI_API_KEY="your-gemini-api-key"   # Optional for cloud intelligence
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
Swagger UI will be available at: `http://localhost:8000/docs`

### 2. Run with Standalone Python (Zero Dependencies):
```bash
cd backend
python3 main.py
```
Server runs on: `http://localhost:8000`

### 3. Run Backend Unit Tests:
```bash
python3 backend/test_backend.py
```

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | Service root and platform status |
| `GET` | `/health` | Health check for container monitoring |
| `POST` | `/api/v1/verify` | Analyze claims, messages, or news text |
| `POST` | `/api/v1/inspect-url` | Security & SSRF inspection for web links |
| `POST` | `/api/v1/chat` | Conversational cyber safety assistant |
