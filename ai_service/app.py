# FastAPI AI microservice for product suggestions
# Vietnamese comments for clarity
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List

app = FastAPI(title="BizFlow AI Service", version="1.0.0")

class ProductIn(BaseModel):
    id: int
    code: str
    name: str
    category: str | None = None

class SuggestRequest(BaseModel):
    products: List[ProductIn]

class Suggestion(BaseModel):
    product_id: int
    suggested_ids: List[int]
    reason: str

class SuggestResponse(BaseModel):
    suggestions: List[Suggestion]

@app.get("/health")
async def health():
    return {"status": "ok"}

@app.post("/suggest-products", response_model=SuggestResponse)
async def suggest_products(req: SuggestRequest):
    # Dummy heuristic: suggest next two IDs in list order
    suggestions: List[Suggestion] = []
    all_ids = [p.id for p in req.products]
    for p in req.products:
        idx = all_ids.index(p.id)
        next_ids = []
        if idx + 1 < len(all_ids):
            next_ids.append(all_ids[idx + 1])
        if idx + 2 < len(all_ids):
            next_ids.append(all_ids[idx + 2])
        suggestions.append(Suggestion(
            product_id=p.id,
            suggested_ids=next_ids,
            reason="Heuristic based on list proximity"
        ))
    return SuggestResponse(suggestions=suggestions)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
