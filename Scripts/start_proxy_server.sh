#!/bin/sh

if [ -z "$1" ]; then

  echo "Error: No Docker recipes path argument provided"
  exit 1
fi

if [ -z "$PROXY_LOCAL_PROD_PORT" ]; then

  echo "Error: PROXY_LOCAL_PROD_PORT is not set"
  exit 1
fi

if [ -z "$PROXY_LOCAL_PROD_USERNAME" ]; then

  echo "Error: PROXY_LOCAL_PROD_USERNAME is not set"
  exit 1
fi

if [ -z "$PROXY_LOCAL_PROD_PASSWORD" ]; then

  echo "Error: PROXY_LOCAL_PROD_PASSWORD is not set"
  exit 1
fi

if test -e "$1"/passwords; then

  echo "ERROR: Squid user passwords file already exists '$1/passwords'"
  exit 1

else

  if ! sudo htpasswd -b -c "$1"/passwords "$PROXY_LOCAL_PROD_USERNAME" "$PROXY_LOCAL_PROD_PASSWORD"; then

    echo "Error: Failed to create Squid user"
    exit 1
  fi
fi

if cd "$1" && docker login && \
  PROXY_LOCAL_PROD_PORT="$PROXY_LOCAL_PROD_PORT" PROXY_LOCAL_PROD_USERNAME="$PROXY_LOCAL_PROD_USERNAME" \
   PROXY_LOCAL_PROD_PASSWORD="$PROXY_LOCAL_PROD_PASSWORD" \
   docker compose up -d && docker ps; then

  if sleep 10 && curl -x "http://127.0.0.1:$PROXY_LOCAL_PROD_PORT" -U "$PROXY_LOCAL_PROD_USERNAME:$PROXY_LOCAL_PROD_PASSWORD" https://www.github.com; then

    echo "Squid proxy is running and accessible at http://127.0.0.1:$PROXY_LOCAL_PROD_PORT"

  else

    echo "Error: Failed to access GitHub website using proxy."
    exit 1
  fi

else

  echo "Error: Failed to start squid container."
  exit 1
fi