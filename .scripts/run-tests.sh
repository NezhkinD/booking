#!/bin/bash

# Hotel Booking System - Test Runner Script
# This script runs all tests and generates coverage reports

set -e

echo "================================================"
echo "  Hotel Booking System - Running Tests"
echo "================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}Step 1: Running Booking Service tests...${NC}"
cd booking-service
mvn clean test jacoco:report
BOOKING_EXIT_CODE=$?
cd ..

echo ""
echo -e "${BLUE}Step 2: Running Hotel Service tests...${NC}"
cd hotel-service
mvn clean test jacoco:report
HOTEL_EXIT_CODE=$?
cd ..

echo ""
echo "================================================"
echo "  Test Results"
echo "================================================"

if [ $BOOKING_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ Booking Service: All tests passed${NC}"
else
    echo -e "${YELLOW}⚠️  Booking Service: Some tests failed${NC}"
fi

if [ $HOTEL_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ Hotel Service: All tests passed${NC}"
else
    echo -e "${YELLOW}⚠️  Hotel Service: Some tests failed${NC}"
fi

echo ""
echo "================================================"
echo "  Coverage Reports"
echo "================================================"
echo ""
echo "HTML reports generated at:"
echo "  📊 Booking Service: booking-service/target/site/jacoco/index.html"
echo "  📊 Hotel Service:   hotel-service/target/site/jacoco/index.html"
echo ""
echo "To view reports:"
echo "  xdg-open booking-service/target/site/jacoco/index.html"
echo "  xdg-open hotel-service/target/site/jacoco/index.html"
echo ""
echo "Detailed analysis: COVERAGE_REPORT.md"
echo ""

# Copy reports to coverage-reports directory
echo -e "${BLUE}Copying reports to coverage-reports/...${NC}"
mkdir -p coverage-reports
cp -r booking-service/target/site/jacoco coverage-reports/booking-service
cp -r hotel-service/target/site/jacoco coverage-reports/hotel-service
echo -e "${GREEN}✅ Reports copied${NC}"

echo ""
echo "================================================"
exit $(($BOOKING_EXIT_CODE + $HOTEL_EXIT_CODE))
