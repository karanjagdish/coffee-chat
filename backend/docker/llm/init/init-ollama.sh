#!/bin/sh

# Copy models from host to container
ollama models pull ${OLLAMA_MODEL}
ollama serve