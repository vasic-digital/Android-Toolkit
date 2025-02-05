#!/bin/sh

token="$2"
server_token="$1"

echo "Server token: $server_token"
echo "Client token: $token"
echo "- - -"

curl -X POST -H "Authorization: key=$server_token" -H "Content-Type: application/json" -d "{
      \"to\": \"$token\",
      \"notification\": {
        \"title\": \"Hello world\",
        \"body\": \"Lorem ipsum ...\"
      }
    }" https://fcm.googleapis.com/fcm/send