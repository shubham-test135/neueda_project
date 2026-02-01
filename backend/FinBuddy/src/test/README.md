# 🧪 FinBuddy Backend Test Suite

## Quick Start

### Run All Tests

```bash
mvn clean test
```

### Run with Coverage

```bash
mvn clean test jacoco:report
# View report: target/site/jacoco/index.html
```

## Test Structure

```
📁 src/test/java/com/example/FinBuddy/
├── 📄 FinBuddyApplicationTests.java          # Integration test
├── 📄 ServiceTestSuite.java                  # Suite runner
└── 📁 services/
    ├── 📄 PortfolioServiceTest.java          # ✅ 10 tests
    ├── 📄 AssetServiceTest.java              # ✅ 12 tests
    ├── 📄 StockPriceServiceTest.java         # ✅ 13 tests
    └── 📄 PDFReportServiceTest.java          # ✅ 8 tests
```

## Technologies

- **JUnit 5** - Testing framework
- **Mockito** - Mocking dependencies
- **AssertJ** - Fluent assertions
- **H2 Database** - In-memory test database

## Example Test

```java
@Test
@DisplayName("Should create portfolio successfully")
void shouldCreatePortfolio() {
    // Arrange
    when(portfolioRepository.save(any())).thenReturn(testPortfolio);

    // Act
    Portfolio created = portfolioService.createPortfolio(newPortfolio);

    // Assert
    assertThat(created).isNotNull();
    assertThat(created.getId()).isEqualTo(1L);
    verify(portfolioRepository, times(1)).save(any());
}
```

## Coverage Summary

| Service           | Tests   | Coverage |
| ----------------- | ------- | -------- |
| PortfolioService  | 10      | 85%+     |
| AssetService      | 12      | 85%+     |
| StockPriceService | 13      | 80%+     |
| PDFReportService  | 8       | 75%+     |
| **Total**         | **43+** | **80%+** |

## Run Specific Tests

```bash
# Single test class
mvn test -Dtest=PortfolioServiceTest

# Single test method
mvn test -Dtest=PortfolioServiceTest#shouldCreatePortfolio

# Pattern matching
mvn test -Dtest=*ServiceTest

# Test suite
mvn test -Dtest=ServiceTestSuite
```

## CI/CD Integration

```yaml
# .github/workflows/test.yml
- name: Run tests
  run: mvn clean test

- name: Upload coverage
  uses: codecov/codecov-action@v3
```

---

📚 **Full Documentation**: [TEST_DOCUMENTATION.md](../TEST_DOCUMENTATION.md)
