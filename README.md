# Magpie

Magpie, the Materials-Agnostic Platform for Informatics and Exploration, is designed to simplify the use of machine learning to predict properties of materials.

> **Archive / mirror notice:** This repository is a community-maintained GitHub mirror/archive of the original Magpie project. The owner of this mirror is **not** the original Magpie author or developer. Historical upstream links may no longer be available.

## How do I get Magpie?

### Building from source

Clone this GitHub repository, including the remaining `expr` submodule:

```sh
git clone --recursive https://github.com/zankzeke/magpie.git
cd magpie
./gradlew jar
```

The original upstream `lib/vassal` Bitbucket submodule is no longer retrievable. To keep this archive buildable without that dead submodule, this mirror restores the historical `required-libraries/Vassal.jar` that was stored directly in Magpie before Vassal was converted to a submodule. See [`required-libraries/VASSAL_FALLBACK.md`](required-libraries/VASSAL_FALLBACK.md) for provenance and limitations.

### Precompiled version

The historical precompiled-download URLs are no longer available, so this mirror does not advertise a precompiled package. Build from source using the instructions above.

## How do I learn to use Magpie?

The original hosted documentation URLs are no longer reliable. Archived documentation is included in this repository:

- [Documentation (`doc/index.html`)](doc/index.html)
- [Tutorial (`doc/tutorial.html`)](doc/tutorial.html)

These are the original archived HTML files. GitHub may display an HTML file as source rather than as a rendered website; downloading the `doc/` directory and opening `doc/index.html` or `doc/tutorial.html` locally preserves the original layout and relative links.

## Who can answer my questions?

For problems specific to this mirror/archive, please use [GitHub Issues](https://github.com/zankzeke/magpie/issues). For questions about the original Magpie project, note that this mirror's maintainer is not the original developer and may not be able to provide upstream project support.
