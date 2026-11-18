# Test Generation

When you generate unit test methods, keep in mind the following best practices:

The body of test method should be divided into 3 parts:
- Arrange: Set up any necessary preconditions and inputs.
- Act: Execute the code that is being tested.
- Assert: Verify that the result of the code matches the expectations.
- The parts should be separated with empty line, and start with the following comments:
- // Arrange
- // Act
- // Assert
