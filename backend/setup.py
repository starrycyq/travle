from setuptools import setup, find_packages

with open("requirements.txt") as f:
    requirements = f.read().splitlines()

setup(
    name="travel-assistant-backend",
    version="1.0.0",
    description="AI-powered travel assistant backend",
    packages=find_packages(where="modular_api"),
    install_requires=requirements,
    author="Travel Assistant Team",
    author_email="team@example.com",
    classifiers=[
        "Development Status :: 3 - Alpha",
        "Intended Audience :: Developers",
        "License :: OSI Approved :: MIT License",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
    ],
    python_requires=">=3.8",
)