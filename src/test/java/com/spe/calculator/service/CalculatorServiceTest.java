package com.spe.calculator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorServiceTest {

    private static final double DELTA = 1e-9;
    private final CalculatorService service = new CalculatorService();

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({"2, 3, 5", "-4, 10, 6", "0, 0, 0", "1.5, 2.25, 3.75"})
    void add(double a, double b, double expected) {
        assertEquals(expected, service.add(a, b), DELTA);
    }

    @ParameterizedTest(name = "{0} - {1} = {2}")
    @CsvSource({"10, 4, 6", "3, 5, -2", "-1, -1, 0", "5.5, 0.5, 5"})
    void subtract(double a, double b, double expected) {
        assertEquals(expected, service.subtract(a, b), DELTA);
    }

    @ParameterizedTest(name = "{0} * {1} = {2}")
    @CsvSource({"6, 7, 42", "-3, 4, -12", "9, 0, 0", "2.5, 4, 10"})
    void multiply(double a, double b, double expected) {
        assertEquals(expected, service.multiply(a, b), DELTA);
    }

    @ParameterizedTest(name = "{0} / {1} = {2}")
    @CsvSource({"10, 2, 5", "7, 2, 3.5", "-9, 3, -3", "0, 5, 0"})
    void divide(double a, double b, double expected) {
        assertEquals(expected, service.divide(a, b), DELTA);
    }

    @Test
    @DisplayName("divide by zero throws ArithmeticException")
    void divideByZero() {
        ArithmeticException ex = assertThrows(ArithmeticException.class, () -> service.divide(5, 0));
        assertEquals("Division by zero is not allowed", ex.getMessage());
    }
}
