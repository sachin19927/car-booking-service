$ErrorActionPreference = "Stop"

newman run ".\Car-Booking-Service-Test-Suite.postman_collection.json" `
  -e ".\Car-Booking-Service-Local.postman_environment.json" `
  --reporters cli,junit `
  --reporter-junit-export ".\postman-results.xml"
