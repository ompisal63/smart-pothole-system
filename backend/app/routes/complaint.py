from fastapi import APIRouter, UploadFile, File, Form
from datetime import datetime
import csv
import os
from app.services.email_service import send_confirmation_email
from app.routes.auth import get_current_authority
from fastapi import Depends
from fastapi.responses import FileResponse



router = APIRouter(prefix="/authority", tags=["Authority"])

ALLOWED_STATUS = {"OPEN", "IN_PROGRESS", "RESOLVED", "REJECTED"}
ALLOWED_ASSIGNEES = {"OM", "AKSHATA", "SANKALP", "KHUSHI"}
DATA_FILE = "data/complaints.csv"
IMAGE_DIR = "data/images"
os.makedirs(IMAGE_DIR, exist_ok=True)

@router.post("/complaint")
def register_complaint(
    full_name: str = Form(...),
    email: str = Form(...),
    mobile: str = Form(...),
    latitude: str = Form(...),
    longitude: str = Form(...),
    location_description: str = Form(...),
    image: UploadFile = File(...)
):
    # 1️⃣ Generate backend-owned complaint ID
    complaint_id = f"SP-{int(datetime.utcnow().timestamp())}"

    # 2️⃣ Save image
    image_filename = f"{complaint_id}.jpg"
    image_path = os.path.join(IMAGE_DIR, image_filename)


    with open(image_path, "wb") as f:
        f.write(image.file.read())

    # 3️⃣ Save to CSV
    file_exists = os.path.exists(DATA_FILE)

    with open(DATA_FILE, mode="a", newline="", encoding="utf-8") as csvfile:
        writer = csv.writer(csvfile)

        if not file_exists or os.stat(DATA_FILE).st_size == 0:
            writer.writerow([
                 "complaint_id",
                 "full_name",
                 "email",
                 "mobile",
                 "latitude",
                 "longitude",
                 "location_description",
                 "image_path",
                 "timestamp",
                 "status",
                 "assigned_to",
                 "assigned_by",
                 "assigned_at",
                 "last_updated",
                 "activity_log"
                ])

        writer.writerow([
            complaint_id,
            full_name,
            email,
            mobile,
            latitude,
            longitude,
            location_description,
            image_path,
            datetime.utcnow().isoformat(),  # timestamp
            "OPEN",                          # status
            "",                              # assigned_to
            "",                              # assigned_by
            "",                              # assigned_at
            datetime.utcnow().isoformat(),
            f"{datetime.utcnow().isoformat()} | Complaint created"
        ])

    # 4️⃣ Send confirmation email
    send_confirmation_email(
        to_email=email,
        complaint_id=complaint_id,
        location_description=location_description
    )

    # 5️⃣ Return success
    return {
        "status": "success",
        "complaint_id": complaint_id
    }

@router.get("/complaints")
def get_all_complaints(
    authority_id: str = Depends(get_current_authority)
):
    if not os.path.exists(DATA_FILE):
        return {
            "authority_id": authority_id,
            "total_complaints": 0,
            "complaints": []
        }

    complaints = []

    with open(DATA_FILE, mode="r", encoding="utf-8") as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            complaints.append(row)

    return {
        "authority_id": authority_id,
        "total_complaints": len(complaints),
        "complaints": complaints
    }


@router.get("/authority-test")
def authority_test(authority_id: str = Depends(get_current_authority)):
    return {
        "message": "Authorized",
        "authority_id": authority_id
    }

@router.get("/complaint/{complaint_id}")
def get_complaint_detail(
    complaint_id: str,
    authority_id: str = Depends(get_current_authority)
):
    if not os.path.exists(DATA_FILE):
        return {"error": "No complaints found"}

    with open(DATA_FILE, mode="r", encoding="utf-8") as csvfile:
        reader = csv.DictReader(csvfile)

        for row in reader:
            if row["complaint_id"] == complaint_id:

                return {
                    "authority_id": authority_id,
                    "complaint": {
                        "complaint_id": row["complaint_id"],
                        "full_name": row["full_name"],
                        "email": row["email"],
                        "mobile": row["mobile"],
                        "latitude": row["latitude"],
                        "longitude": row["longitude"],
                        "location_description": row["location_description"],
                        "timestamp": row["timestamp"],
                        "status": row["status"],
                        "assigned_to": row["assigned_to"],
                        "assigned_by": row["assigned_by"],
                        "assigned_at": row["assigned_at"],
                        "last_updated": row["last_updated"],
                    },
                    "media": {
                        "image_url": f"/authority/complaint/{complaint_id}/image"
                    },
                    "workflow": {
                        "allowed_status": list(ALLOWED_STATUS),
                        "allowed_assignees": list(ALLOWED_ASSIGNEES)
                    }
                }

    return {"error": "Complaint not found"}


@router.patch("/complaint/{complaint_id}")
def update_complaint(
    complaint_id: str,
    status: str = Form(None),
    assigned_to: str = Form(None),
    authority_id: str = Depends(get_current_authority)
):
    if not os.path.exists(DATA_FILE):
        return {"error": "No complaints found"}

    if status and status not in ALLOWED_STATUS:
        return {"error": "Invalid status value"}

    if assigned_to and assigned_to not in ALLOWED_ASSIGNEES:
        return {"error": "Invalid assignee"}

    updated_rows = []
    found = False
    now = datetime.utcnow().isoformat()

    with open(DATA_FILE, mode="r", encoding="utf-8") as csvfile:
        reader = csv.DictReader(csvfile)
        fieldnames = reader.fieldnames

        for row in reader:
            if row["complaint_id"] == complaint_id:
                found = True

                activity = (row.get("activity_log") or "").strip()

                # STATUS CHANGE
                if status and status != row["status"]:
                    old_status = row["status"]
                    row["status"] = status
                    activity += f"\n{now} | STATUS | {authority_id} | {old_status} -> {status}"

                # ASSIGNMENT CHANGE
                if assigned_to and assigned_to != row["assigned_to"]:
                    row["assigned_to"] = assigned_to
                    row["assigned_by"] = authority_id
                    row["assigned_at"] = now
                    activity += f"\n{now} | ASSIGNED | {authority_id} -> {assigned_to}"

                row["activity_log"] = activity.strip()
                row["last_updated"] = now

            updated_rows.append(row)

    if not found:
        return {"error": "Complaint not found"}

    with open(DATA_FILE, mode="w", newline="", encoding="utf-8") as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(updated_rows)

    return {
        "status": "updated",
        "complaint_id": complaint_id,
        "updated_by": authority_id
    }


@router.get("/complaint/{complaint_id}/image")
def get_complaint_image(complaint_id: str,):
    if not os.path.exists(DATA_FILE):
        return {"error": "No complaints found"}

    with open(DATA_FILE, mode="r", encoding="utf-8") as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            if row["complaint_id"] == complaint_id:
                image_path = row["image_path"]

                if not os.path.exists(image_path):
                    return {"error": "Image not found"}

                return FileResponse(
                    path=image_path,
                    media_type="image/jpeg",
                    filename=f"{complaint_id}.jpg"
                )

    return {"error": "Complaint not found"}



