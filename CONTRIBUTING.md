# Contributing to Rosemary

Bug reports, datetimes that Rosemary gets wrong, and pull requests are welcome.

## Reporting a datetime that parses wrong
Open an issue with the string, the result you expected, and the result you got.  Include the date order and time zone settings if you changed them.

## Making a change
1. Build and test with `./gradlew build`.  It needs a JDK 17 or newer, and downloads the JDK 17 toolchain if needed.
2. Add test cases for your change.  Most tests are tables of inputs and expected results, in `lib/src/test/java/com/lemmingapex/rosemary/`, one class per topic, e.g. `TimeZoneParsingTest`.  A new case is usually one line.
   - A datetime as a real program, protocol or person writes it goes in [`corpus.txt`](lib/src/test/resources/com/lemmingapex/rosemary/corpus.txt), grouped by where it comes from.
   - A new format that a `DateTimeFormatter` pattern can write goes in `RoundTripTest`, which checks it with a thousand random datetimes.
3. Keep `./gradlew build` passing.  It fails on compiler or Javadoc warnings, or if test coverage drops below 95% of lines or 90% of branches.
4. If you change an example in the README, change `ReadmeExamplesTest` too.
5. Add a line to `CHANGELOG.md`.

## Code layout
- `RosemaryDateTimeParser` is the public API and its builder.
- `Tokenizer` splits a string into tokens, and `PatternFreeParser` decides what each token is.
- `DateTimeFields` holds what was found, and fills in missing fields.
- `TimeZoneNames` holds the time zone abbreviations and region ids.
- `FormatParser` parses with `DateTimeFormatter` patterns.

Only the `com.lemmingapex.rosemary` package's public classes are API.  Everything else is package-private, and can change.

## Style
Tabs for indentation, and comments that explain why.  Match the code around your change.
