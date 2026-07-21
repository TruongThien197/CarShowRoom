# GearShift Pro CarShowRoom - Business Requirements Document

## 1. Document Information

| Item | Value |
| --- | --- |
| Project | GearShift Pro CarShowRoom |
| Platform | Java Spring Boot MVC, Thymeleaf, SQL Server |
| Document Type | Business Requirements Document |
| Prepared For | HSF302 course project |
| Current Scope Source | Existing source code, templates, entities, controllers, services, and seed data |

## 2. Business Overview

GearShift Pro CarShowRoom is a web-based showroom platform that supports two connected business lines:

- Selling auto parts through a product catalog, cart, checkout, and order tracking flow.
- Booking vehicle repair and maintenance services through vehicle profile management, service selection, booking history, and booking status management.

The system is designed for three main user groups: customers, staff, and administrators. Customers browse products, place orders, manage vehicles, and create repair bookings. Staff handle operational order and booking processing. Administrators manage platform data, users, products, categories, services, bookings, orders, and revenue overview.

## 3. Business Goals

- Provide customers with a simple online channel to buy auto parts.
- Allow customers to schedule vehicle repair or maintenance appointments.
- Give staff a focused dashboard to process orders and bookings.
- Give administrators full control over users, catalog data, service data, platform operations, and revenue visibility.
- Keep the platform structured around MVC responsibilities: controllers handle request flow, services handle business logic, repositories handle persistence, and Thymeleaf templates handle UI rendering.

## 4. Stakeholders

| Stakeholder | Interest |
| --- | --- |
| Customer | Browse products, manage cart, checkout, track orders, manage vehicles, book services. |
| Staff | Process pending orders and bookings, update operational statuses. |
| Administrator | Manage users, categories, products, services, orders, bookings, and revenue. |
| Showroom Business Owner | Increase parts sales, reduce manual booking work, monitor business performance. |
| Development Team | Maintain the Spring Boot MVC system and database consistency. |

## 5. User Roles And Access

| Role | Access |
| --- | --- |
| Guest | Home page, product catalog, product details, login, registration, static assets. |
| Customer | Account profile, cart, checkout, order history, booking pages, vehicle pages. |
| Staff | Staff dashboard, order status updates, booking status updates. |
| Admin | Admin dashboard, user management, category management, product management, service management, booking management, order management, revenue summary. |

Role access is enforced by Spring Security:

- `/admin/**` requires `ADMIN`.
- `/staff/**` requires `STAFF`.
- `/cart/**`, `/orders/**`, `/booking/**`, `/vehicles/**`, `/account/**` require `CUSTOMER`.
- `/`, `/shop`, `/products/**`, `/auth/**`, and static resources are public.

## 6. In Scope

- Authentication with login, register, and logout.
- Role-based navigation and access control.
- Public product browsing and product detail viewing.
- Customer cart management.
- Customer checkout and order history.
- Customer vehicle management.
- Customer service booking creation, detail viewing, history viewing, and cancellation.
- Staff operational dashboard for orders and bookings.
- Admin dashboard with revenue and operational metrics.
- Admin CRUD for users, categories, products, and repair services.
- Admin order and booking management.
- SQL Server schema and seed data support.

## 7. Out Of Scope

- Online payment gateway integration.
- Real-time staff notifications.
- Email or SMS booking confirmation.
- Product compatibility search by vehicle model in the current UI.
- Advanced inventory receiving, supplier management, and purchase orders.
- Multi-branch showroom support.
- REST API for external clients.

## 8. Functional Requirements

### 8.1 Authentication And Account

| ID | Requirement | Priority |
| --- | --- | --- |
| AUTH-01 | The system shall allow guests to register a customer account with email, password, full name, phone, and address. | High |
| AUTH-02 | The system shall validate login and registration input. | High |
| AUTH-03 | The system shall allow users to log in by email and password. | High |
| AUTH-04 | The system shall redirect users after login based on role: admin to admin dashboard, staff to staff dashboard, customer to home. | High |
| AUTH-05 | The system shall prevent locked users from logging in. | High |
| AUTH-06 | The system shall expose customer profile summary including order count and booking count. | Medium |

### 8.2 Product Catalog

| ID | Requirement | Priority |
| --- | --- | --- |
| PROD-01 | The system shall show active products in the public catalog. | High |
| PROD-02 | The system shall allow filtering products by category. | Medium |
| PROD-03 | The system shall allow searching products by keyword. | Medium |
| PROD-04 | The system shall show product detail including category, price, stock, status, image, and description. | High |
| PROD-05 | The system shall support pagination for product browsing. | Medium |

### 8.3 Cart

| ID | Requirement | Priority |
| --- | --- | --- |
| CART-01 | Customers shall be able to add active products to cart. | High |
| CART-02 | Customers shall be able to update item quantity. | High |
| CART-03 | Customers shall be able to remove items from cart. | High |
| CART-04 | The system shall validate stock before adding or updating quantities. | High |
| CART-05 | The system shall calculate cart subtotal from product price and quantity. | High |

### 8.4 Checkout And Orders

| ID | Requirement | Priority |
| --- | --- | --- |
| ORD-01 | Customers shall be able to checkout using items currently in cart. | High |
| ORD-02 | The system shall create an order with shipping address, total amount, order date, and `PENDING` status. | High |
| ORD-03 | The system shall create order detail lines for each cart item. | High |
| ORD-04 | The system shall reduce product stock after successful checkout. | High |
| ORD-05 | The system shall clear the customer's cart after successful checkout. | High |
| ORD-06 | Customers shall be able to view their order history. | High |
| ORD-07 | Staff and admin shall be able to update order status. | High |
| ORD-08 | Admin shall be able to filter and view order details. | Medium |

### 8.5 Vehicles

| ID | Requirement | Priority |
| --- | --- | --- |
| VEH-01 | Customers shall be able to view their registered vehicles. | High |
| VEH-02 | Customers shall be able to add a vehicle by selecting car model and entering license plate. | High |
| VEH-03 | Customers shall be able to delete their own vehicles. | Medium |
| VEH-04 | The system shall prevent customers from deleting or modifying vehicles owned by another user. | High |

### 8.6 Service Booking

| ID | Requirement | Priority |
| --- | --- | --- |
| BOOK-01 | Customers shall be able to create a repair booking. | High |
| BOOK-02 | A booking shall include customer, optional vehicle, booking date, time slot, service, status, and notes. | High |
| BOOK-03 | The system shall prevent booking dates in the past. | High |
| BOOK-04 | The system shall set new bookings to `PENDING`. | High |
| BOOK-05 | Customers shall be able to view their booking history. | High |
| BOOK-06 | Customers shall be able to view booking details and selected services. | High |
| BOOK-07 | Customers shall be able to cancel bookings unless completed. | High |
| BOOK-08 | Customers shall receive a confirmation prompt before cancelling a booking. | Medium |
| BOOK-09 | Staff and admin shall be able to approve, cancel, or complete bookings by updating status. | High |

### 8.7 Admin Management

| ID | Requirement | Priority |
| --- | --- | --- |
| ADM-01 | Admin shall view platform metrics: users, products, orders, bookings, order revenue, booking revenue, and total revenue. | High |
| ADM-02 | Admin shall view recent orders and recent bookings. | Medium |
| ADM-03 | Admin shall view monthly order chart data for the latest six months. | Medium |
| ADM-04 | Admin shall manage users, including create, edit, view details, search, filter by role/status, and change status. | High |
| ADM-05 | Admin shall manage product categories. | High |
| ADM-06 | Admin shall manage products including create, edit, delete, and status update. | High |
| ADM-07 | Admin shall manage repair services including create, edit, delete, and list. | High |
| ADM-08 | Admin shall view booking list, booking details, and update booking statuses. | High |
| ADM-09 | Admin shall view order list, order details, and update order statuses. | High |

### 8.8 Staff Operations

| ID | Requirement | Priority |
| --- | --- | --- |
| STF-01 | Staff shall access a dedicated dashboard separate from admin. | High |
| STF-02 | Staff shall see operational order and booking data. | High |
| STF-03 | Staff shall update order statuses. | High |
| STF-04 | Staff shall update booking statuses. | High |

## 9. Business Rules

| Area | Rule |
| --- | --- |
| Authentication | Email must be unique. Password is stored as BCrypt hash. |
| User status | Locked users cannot log in. |
| Product status | Only active products should be sold publicly. |
| Cart | Quantity must be at least 1 unless the item is removed. |
| Stock | Checkout must fail if requested quantity exceeds available stock. |
| Order creation | A new order starts as `PENDING`. |
| Order revenue | Revenue dashboard counts delivered orders only. |
| Booking creation | Booking date cannot be earlier than the current date. |
| Booking status | A new booking starts as `PENDING`. |
| Booking cancellation | Completed bookings cannot be cancelled by customer. |
| Booking revenue | Revenue dashboard counts completed service bookings only. |
| Vehicle ownership | Customers can only use or delete vehicles that belong to them. |

## 10. Status Definitions

### 10.1 User Status

| Status | Meaning |
| --- | --- |
| `ACTIVE` | User can access the system. |
| `LOCK` | User account is locked and cannot log in. |

### 10.2 Product Status

| Status | Meaning |
| --- | --- |
| `ACTIVE` | Product is visible and sellable. |
| `INACTIVE` | Product is not actively sold. |

### 10.3 Order Status

| Status | Meaning |
| --- | --- |
| `PENDING` | Order has been placed and awaits processing. |
| `PROCESSING` | Order is being prepared. |
| `SHIPPED` | Order has been shipped. |
| `DELIVERED` | Order has been delivered and counts as revenue. |
| `CANCELLED` | Order has been cancelled. |

### 10.4 Booking Status

| Status | Meaning |
| --- | --- |
| `PENDING` | Booking request is waiting for review. |
| `APPROVED` | Booking has been accepted. |
| `COMPLETED` | Booking service has been completed and counts as booking revenue. |
| `CANCELLED` | Booking has been cancelled. |

## 11. Data Requirements

| Entity | Purpose | Key Fields |
| --- | --- | --- |
| User | Stores account, role, and contact information. | email, passwordHash, fullName, phone, address, role, status |
| Category | Groups products. | categoryName, description |
| Product | Stores parts for sale. | category, productName, description, price, stockQuantity, imageUrl, status |
| CartItem | Stores customer cart lines. | user, product, quantity |
| Order | Stores checkout transaction summary. | user, orderDate, totalAmount, shippingAddress, status |
| OrderDetail | Stores products inside an order. | order, product, quantity, unitPrice |
| Service | Stores repair or maintenance services. | serviceName, description, price |
| Booking | Stores repair appointment request. | user, vehicle, bookingDate, timeSlot, status, notes |
| BookingDetail | Stores services selected for a booking. | booking, service, actualPrice |
| CarModel | Stores vehicle model master data. | brand, modelName, year |
| Vehicle | Stores customer vehicles. | user, carModel, licensePlate |
| ProductCompatibility | Links products to compatible car models. | product, carModel |

## 12. Page And Route Map

| Area | Routes | Pages |
| --- | --- | --- |
| Public | `/`, `/shop`, `/products`, `/products/{id}` | Home, product catalog, product detail |
| Auth | `/auth/login`, `/auth/register`, `/auth/logout` | Login, register |
| Customer cart | `/cart`, `/cart/add`, `/cart/update`, `/cart/remove` | Cart |
| Customer orders | `/orders/checkout`, `/orders` | Checkout, order history |
| Customer booking | `/booking`, `/booking/create`, `/booking/my-bookings`, `/booking/{id}`, `/booking/{id}/cancel` | Booking create, history, detail |
| Customer vehicles | `/vehicles`, `/vehicles/add`, `/vehicles/{id}/delete` | Vehicle list, add vehicle |
| Account | `/account` | Customer profile |
| Staff | `/staff`, `/staff/orders/status`, `/staff/bookings/status` | Staff dashboard |
| Admin dashboard | `/admin` | Overview, revenue, recent activity |
| Admin users | `/admin/users`, `/admin/users/create`, `/admin/users/edit/{id}`, `/admin/users/{id}` | User list, create, edit, detail |
| Admin categories | `/admin/categories`, `/admin/categories/create`, `/admin/categories/edit/{id}`, `/admin/categories/delete/{id}` | Category list, create, edit |
| Admin products | `/admin/products`, `/admin/products/create`, `/admin/products/edit/{id}`, `/admin/products/delete/{id}`, `/admin/products/status` | Product list, create, edit |
| Admin services | `/admin/services`, `/admin/services/create`, `/admin/services/edit/{id}`, `/admin/services/delete/{id}` | Service list, create, edit |
| Admin bookings | `/admin/bookings`, `/admin/bookings/{id}`, `/admin/bookings/status` | Booking list, detail |
| Admin orders | `/admin/orders`, `/admin/orders/{id}`, `/admin/orders/{id}/status`, `/admin/orders/status` | Order list, detail |

## 13. Non-Functional Requirements

| Category | Requirement |
| --- | --- |
| Security | Role-based authorization must protect admin, staff, and customer pages. |
| Passwords | Passwords must be hashed with BCrypt. |
| Usability | Customer, staff, and admin users should see different interfaces based on role. |
| Maintainability | Business logic should be placed in service classes where practical. |
| Database | The system uses SQL Server with JPA/Hibernate and seed data. |
| Availability | The system should run locally on configured port `8386`. |
| Data integrity | Orders, bookings, and details should maintain referential integrity with users, products, services, and vehicles. |
| Localization | Current UI target language is English. |

## 14. Acceptance Criteria

- Guest can browse public catalog without login.
- Customer can register, log in, add products to cart, checkout, and view order history.
- Customer checkout decreases product stock and clears cart.
- Customer can add a vehicle, create a booking, view booking history, view booking detail, and cancel eligible bookings.
- Admin can access admin dashboard and see total users, products, orders, bookings, and revenue.
- Admin can manage users, categories, products, services, orders, and bookings.
- Staff can access staff dashboard and update order or booking statuses.
- Unauthorized users are redirected away from protected pages.
- Demo seed data creates admin, staff, customer, products, services, and sample operational data.

## 15. Assumptions

- The project is intended for a single showroom operation.
- Demo accounts use `123456` as the seeded password.
- Order payment is handled outside the system or assumed to happen after order placement.
- Service booking payment is not collected during booking creation.
- Admin is trusted to manage product, category, and service master data correctly.
- Booking time slots are fixed values in the UI rather than dynamically generated from staff availability.

## 16. Open Issues And Recommendations

- Some controller methods still directly use repositories; for cleaner MVC, admin product, category, booking, and service operations should move into service classes.
- Some old or unused templates may remain, such as legacy booking or shop pages; these should be reviewed before final submission.
- CSRF is currently disabled; this is acceptable for a classroom demo but should be enabled for production.
- Product compatibility data exists in the database model but is not fully exposed in the customer UI.
- Booking capacity and staff availability rules are not implemented yet.
- Order and booking status values should ideally be centralized as enums to prevent invalid status input.
- Flash messages and exception messages should be reviewed to ensure all are in English and readable.

## 17. Suggested Future Enhancements

- Add email confirmation for orders and bookings.
- Add admin car model management UI.
- Add vehicle-based product compatibility filtering.
- Add calendar availability for service bookings.
- Add payment method selection and payment status tracking.
- Add inventory low-stock alerts for admin and staff.
- Add customer-facing order detail page.
- Add audit trail for admin and staff status changes.
