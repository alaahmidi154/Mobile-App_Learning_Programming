FROM python:3.11-slim

# Never write .pyc files (the container filesystem is read-only anyway)
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# Non-root user: student code never runs as root
RUN useradd --create-home --uid 10001 --shell /usr/sbin/nologin runner

WORKDIR /app
USER runner

# The student's code is mounted read-only at /app/main.py by runner.py
CMD ["python", "/app/main.py"]
