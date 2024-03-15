# Java Default Parameter Values

## Introduction

Java, unlike some other programming languages (e.g., Python or Kotlin), does not support default parameter values in its methods, requiring developers to use method overloading or builder patterns to achieve similar functionality. This project introduces a way to add default parameter values to Java methods without resorting to method overloading, making code cleaner and more maintainable.

## Features

- **Simplicity:** Easily define default values for method parameters directly in the method signature.
- **Compatibility:** Works with existing Java code without the need for extensive refactoring.
- **Annotation-based:** Utilizes custom annotations to define default parameter values, ensuring readability and ease of use.

## How It Works

This project uses Java annotations and bytecode manipulation at compile time to inject code that simulates default parameter values. When a method with default parameters is called without some of the parameters, the bytecode manipulation ensures that default values are automatically used.

## Installation

## Usage

## Contributing

## License

This project is licensed under the MIT License - see the LICENSE file for details.

