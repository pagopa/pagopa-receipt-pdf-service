# Docker Environment 🐳
`run_docker.sh` is a script to launch the image of this microservice and all the dependencies on Docker.

## How to use 💻
You can use `local`, `dev`, `uat` or `prod` images

Precondition: `az login`

`sh ./run_docker.sh <local|dev|uat|prod>`

---

ℹ️ _Note_: for **PagoPa ACR** is **required** the login `az acr login -n <acr-name>`

ℹ️ _Note_: If you run the script without the parameter, `local` is used as default.
