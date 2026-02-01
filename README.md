# 💰 FinBuddy - Financial Portfolio Management Dashboard

A comprehensive financial portfolio management system with **Java Spring Boot 3** backend and modern **modular frontend** architecture.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.10-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## ✨ Features

### 💼 Portfolio Management

- **Multi-Asset Support**: Stocks, Bonds, Mutual Funds, SIPs
- **Multi-Currency**: USD, EUR, GBP, INR with automatic conversion
- **Wishlist/Sandbox**: Test investment strategies without real commitment
- **Benchmarking**: Compare against market indices (S&P 500, NIFTY 50)
- **PDF Reports**: Professional downloadable reports with charts

### 📊 Dashboard & Analytics

- **Real-time Metrics**: Total value, gain/loss, ROI tracking
- **Interactive Charts**: Allocation pie chart, growth trends, performance bars
- **Search & Filter**: Quick asset lookup by name or symbol
- **Performance History**: Track portfolio value over time

### 🔧 Technical Highlights

- **RESTful API**: Well-documented endpoints with Swagger UI
- **Modular Frontend**: Component-based architecture with ES6 modules
- **Testing**: 50+ unit & integration tests (JUnit 5 + Mockito)
- **Market Data**: Integration with Finnhub & Alpha Vantage APIs

## 🏗️ Architecture

### Backend (Spring Boot MVC)

```
backend/FinBuddy/src/main/java/com/example/FinBuddy/
├── entities/          # JPA Entity models
│   ├── Portfolio.java
│   ├── Asset.java (Abstract)
│   ├── Stock.java
│   ├── Bond.java
│   ├── MutualFund.java
│   ├── SIP.java
│   └── PortfolioHistory.java
├── repositories/      # Spring Data JPA repositories
│   ├── PortfolioRepository.java
│   ├── AssetRepository.java
│   └── PortfolioHistoryRepository.java
├── services/          # Business logic layer
│   ├── PortfolioService.java
│   ├── AssetService.java
│   ├── StockPriceService.java
│   └── PDFReportService.java
├── controllers/       # REST API controllers
│   ├── PortfolioController.java
│   ├── AssetController.java
│   ├── MarketDataController.java
│   └── ReportController.java
├── dto/              # Data Transfer Objects
│   ├── DashboardSummaryDTO.java
│   ├── AssetAllocationDTO.java
│   └── AssetPerformanceDTO.java
└── config/           # Configuration classes
    └── WebClientConfig.java
```

### Frontend

```
backend/FinBuddy/src/main/resources/static/
├── index.html        # Main dashboard page
├── css/
│   └── styles.css    # Responsive CSS styling
└── js/
    └── app.js        # JavaScript for API calls & charts
```

---

## 🚀 Quick Start

### Prerequisites

- **Java 21+** ([Download](https://adoptium.net/))
- **Maven 3.6+** (Included: use `./mvnw` wrapper)
- **MySQL 8.0+** or **PostgreSQL 13+**
- **IDE**: IntelliJ IDEA / VS Code / Eclipse

### 1️⃣ Database Setup

**Option A: MySQL**

```sql
CREATE DATABASE finbuddy_db;
```

**Option B: PostgreSQL**

```sql
CREATE DATABASE finbuddy_db;
```

### 2️⃣ Configure Application

Edit `backend/FinBuddy/src/main/resources/application.properties`:

**MySQL (Default):**

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/finbuddy_db
spring.datasource.username=root
spring.datasource.password=
```

**PostgreSQL:**

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/finbuddy_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### 3️⃣ Build & Run

```bash
cd backend/FinBuddy

# Using Maven wrapper (recommended)
./mvnw clean spring-boot:run

# Or with installed Maven
mvn clean spring-boot:run
```

### 4️⃣ Access Application

| Service          | URL                                   |
| ---------------- | ------------------------------------- |
| 🏠 **Dashboard** | http://localhost:8081/                |
| 📚 **API Docs**  | http://localhost:8081/swagger-ui.html |
| 💓 **Health**    | http://localhost:8081/actuator/health |

---

## 🧪 Testing

### Run All Tests

```bash
cd backend/FinBuddy
mvn clean test
```

### Run with Coverage Report

```bash
mvn clean test jacoco:report
# View report: target/site/jacoco/index.html
```

### Test Suite Overview

- ✅ **50+ Tests**: Unit + Integration coverage
- 🎯 **Services**: Portfolio, Asset, StockPrice, PDFReport
- 🌐 **Controllers**: Portfolio, MarketData REST endpoints
- 🗄️ **Database**: H2 in-memory for isolated tests

See [backend/FinBuddy/src/test/README.md](backend/FinBuddy/src/test/README.md) for details.

---

## 📡 API Endpoints

### Portfolio Management

```
GET    /api/portfolios                    # Get all portfolios
GET    /api/portfolios/{id}               # Get portfolio by ID
POST   /api/portfolios                    # Create new portfolio
PUT    /api/portfolios/{id}               # Update portfolio
DELETE /api/portfolios/{id}               # Delete portfolio
GET    /api/portfolios/{id}/dashboard     # Get dashboard summary
GET    /api/portfolios/{id}/history       # Get portfolio history
POST   /api/portfolios/{id}/recalculate   # Recalculate metrics
```

### Asset Management

```
GET    /api/assets                              # Get all assets
GET    /api/assets/{id}                         # Get asset by ID
GET    /api/assets/portfolio/{portfolioId}      # Get assets by portfolio
GET    /api/assets/search?query={term}          # Search assets
POST   /api/assets/stocks/{portfolioId}         # Create stock
POST   /api/assets/bonds/{portfolioId}          # Create bond
POST   /api/assets/mutualfunds/{portfolioId}    # Create mutual fund
POST   /api/assets/sips/{portfolioId}           # Create SIP
PUT    /api/assets/{id}                         # Update asset
PATCH  /api/assets/{id}/price?newPrice={price}  # Update price
DELETE /api/assets/{id}                         # Delete asset
```

### Market Data

```
GET  /api/market/price/{symbol}                # Get stock price
POST /api/market/prices/batch                  # Get batch prices
GET  /api/market/exchange-rate?from=USD&to=EUR # Get exchange rate
GET  /api/market/benchmark/{symbol}            # Get index value
```

### Reports

```
GET  /api/reports/portfolio/{portfolioId}/pdf  # Download PDF report
```

## 🎨 UI Components

### Dashboard Features

1. **Summary Cards**: Total value, investment, gain/loss, asset count
2. **Asset Allocation Chart**: Pie chart showing distribution by type
3. **Portfolio Growth Chart**: Line chart tracking value over time
4. **Performance Chart**: Bar chart of top performing assets
5. **Asset Table**: Searchable table with CRUD operations
6. **Wishlist Section**: Sandbox for simulating investments
7. **Benchmark Comparison**: Compare against S&P 500, NIFTY 50

## 💡 Usage Guide

### Create Portfolio

1. Navigate to dashboard
2. Click **"New Portfolio"**
3. Enter name, description, currency (USD/EUR/GBP/INR)
4. Submit

### Add Assets

1. Select portfolio
2. Click **"Add Asset"**
3. Choose type: Stock / Bond / Mutual Fund / SIP
4. Fill details (symbol, quantity, purchase price)
5. _Optional_: Enable "Wishlist" for simulation
6. Save

### Track Performance

- View real-time metrics on dashboard
- Analyze charts: allocation, growth, top performers
- Search/filter assets
- Download PDF report

---

## 🔧 Configuration

### Stock Market API Integration

Configure API keys in `application.properties`:

```properties
# Finnhub (Primary)
finnhub.api.key=your_finnhub_key_here
finnhub.api.enabled=true

# Alpha Vantage (Fallback)
alphavantage.api.key=your_alphavantage_key_here
```

**Get Free API Keys:**

- [Finnhub](https://finnhub.io/register) - 60 calls/min free tier
- [Alpha Vantage](https://www.alphavantage.co/support/#api-key) - 5 calls/min free tier

### Change Server Port

```properties
server.port=8082
```

## 📦 Dependencies

**Backend:**

- Spring Boot 3.5.10
- Spring Data JPA
- MySQL Connector / PostgreSQL Driver
- Lombok
- iText 7 (PDF generation)
- Spring WebFlux (HTTP client)
- Springdoc OpenAPI (API documentation)

**Frontend:**

- Chart.js 4.4.0
- Vanilla JavaScript (ES6+)
- CSS Grid & Flexbox

## 🚦 Troubleshooting

| Issue                          | Solution                                                                                                                 |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------ |
| **Database connection failed** | ✓ Verify MySQL/PostgreSQL is running<br>✓ Check credentials in `application.properties`<br>✓ Ensure `finbuddy_db` exists |
| **Port 8081 in use**           | Change `server.port=8082` in properties file                                                                             |
| **Build fails**                | Run `mvn clean install -U` to update dependencies                                                                        |
| **Tests fail**                 | Check H2 dependency in `pom.xml`<br>Run `mvn clean test -X` for debug logs                                               |
| **API not returning prices**   | Verify API keys are configured<br>Check API rate limits                                                                  |

---

## 📁 Project Structure

```
neueda_project/
├── backend/FinBuddy/
│   ├── src/main/java/com/example/FinBuddy/
│   │   ├── entities/          # JPA models (Portfolio, Asset, etc.)
│   │   ├── repositories/      # Spring Data JPA
│   │   ├── services/          # Business logic
│   │   ├── controllers/       # REST endpoints
│   │   ├── dto/              # Data transfer objects
│   │   └── config/           # App configuration
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── static/           # Frontend files
│   └── src/test/             # Test suite (50+ tests)
└── pom.xml
```

---

## 🛣️ Roadmap

- [x] Core portfolio CRUD operations
- [x] Multi-asset type support
- [x] Interactive dashboard with charts
- [x] PDF report generation
- [x] Comprehensive test coverage
- [x] Stock API integration
- [ ] User authentication (Spring Security)
- [ ] Email notifications for price alerts
- [ ] Dark mode UI theme
- [ ] Mobile app (React Native)
- [ ] Advanced analytics & ML predictions
- [ ] Tax calculation & reporting
- [ ] Multi-user support with roles

---

## 📄 License

This project is licensed under the MIT License.

## 🤝 Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

---

**Built with ❤️ using Spring Boot 3 & Modern Web Technologies**

_Happy Portfolio Management! 💰📈_
