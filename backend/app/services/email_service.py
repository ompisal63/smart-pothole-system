import smtplib
from email.message import EmailMessage

SMTP_SERVER = "smtp.gmail.com"
SMTP_PORT = 587

# 🔴 CHANGE THESE
SENDER_EMAIL = "smartpotholesystem@gmail.com"
APP_PASSWORD = "fjfs insr qtyi lkey"


def send_confirmation_email(to_email: str, complaint_id: str, location_description: str):
    msg = EmailMessage()
    msg["Subject"] = f"Complaint Registered Successfully – {complaint_id}"
    msg["From"] = SENDER_EMAIL
    msg["To"] = to_email

    msg.set_content(f"""
Dear Citizen,

Your pothole complaint has been successfully registered.

Complaint ID: {complaint_id}
Location: {location_description}

Our team will verify and take action shortly.

Regards,
SmartPothole Authority
""")

    try:
        with smtplib.SMTP(SMTP_SERVER, SMTP_PORT) as server:
            server.starttls()
            server.login(SENDER_EMAIL, APP_PASSWORD)
            server.send_message(msg)
            print("📧 Confirmation email sent successfully")

    except Exception as e:
        print("❌ Email sending failed:", e)
