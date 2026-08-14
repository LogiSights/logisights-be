# Architecture

How `logisights-FE` and `logisights-be` fit together, and where the external providers sit.

## System diagram

```mermaid
flowchart LR
    subgraph Client
        FE["logisights-FE\n(Next.js, 4 role dashboards)"]
    end

    subgraph Backend["logisights-be (Quarkus)"]
        AUTH[auth]
        PARCEL[parcel]
        DRIVER[driver]
        PICKUP[pickup]
        PAYMENT[payment]
        ADMIN[admin]
        NOTIF[notification]
    end

    DB[(PostgreSQL\nlogisights)]
    RESEND[[Resend\ntransactional email]]
    MPESA[[Safaricom Daraja\nM-Pesa]]

    FE -->|"JWT bearer, REST/JSON"| AUTH
    FE --> PARCEL
    FE --> DRIVER
    FE --> PICKUP
    FE --> PAYMENT
    FE --> ADMIN

    AUTH --> DB
    PARCEL --> DB
    DRIVER --> DB
    PICKUP --> DB
    PAYMENT --> DB
    ADMIN --> DB

    AUTH --> NOTIF
    PARCEL --> NOTIF
    PAYMENT --> NOTIF
    PICKUP --> NOTIF
    ADMIN --> NOTIF
    NOTIF --> RESEND

    PAYMENT -->|"STK push"| MPESA
    MPESA -->|"callback webhook"| PAYMENT
```

## Request flow: booking a parcel and paying for it

```mermaid
sequenceDiagram
    participant Sender as Sender (FE)
    participant API as parcel module
    participant Pay as payment module
    participant Daraja as M-Pesa Daraja
    participant Mail as notification module

    Sender->>API: POST /parcels (book)
    API->>API: PricingService computes cost server-side
    API-->>Sender: 201, trackingId, cost
    API->>Mail: booking confirmation email

    Sender->>Pay: POST /payments/mpesa/stk-push
    Pay->>Pay: verify sender owns the parcel
    Pay->>Daraja: STK push request
    Daraja-->>Sender: M-Pesa prompt on phone
    Daraja->>Pay: POST /payments/mpesa/callback
    Pay->>Pay: cross-check callback amount against stored payment
    Pay->>API: mark parcel IN_TRANSIT
    Pay->>Mail: payment confirmation email
```

## Why these boundaries

- **Modular monolith, not microservices.** One small team, one deployable, and the domain objects (`parcels`, `users`, `payments`) reference each other constantly; splitting them into services now would mean cross-service transactions for no operational benefit yet. See [CLAUDE.md](CLAUDE.md) for the full reasoning.
- **`payment` and `notification` are the only modules that talk to the outside world.** Daraja and Resend specifics stay inside those two packages so a second payment or email provider could be added without touching `parcel`, `auth`, or `admin`.
- **The frontend never talks to Postgres, Resend, or Daraja directly.** Everything goes through the Quarkus REST API, which is also where authorization (`@RolesAllowed`, ownership checks) and pricing integrity live, since the frontend is untrusted input.
