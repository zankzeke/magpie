# Vassal compatibility fallback

The original Magpie repository eventually used Vassal as the Git submodule `lib/vassal`, hosted at:

`https://bitbucket.org/wolverton-research-group/vassal.git`

That upstream repository is no longer retrievable from the location recorded by Magpie. The `master` snapshot mirrored here referenced Vassal commit:

`ea7d583a8820b81ee17ef9f35f9a455014f809d8`

No trustworthy public mirror containing that exact Vassal commit was identified during the archive repair.

To avoid leaving Magpie with a permanently broken submodule, this mirror restores `Vassal.jar` from **Magpie's own Git history**, from the period before Vassal was converted into a submodule. The JAR blob restored here is:

`7861f67b2020754efb37226d1d3b92792a18eb98`

It was removed when Magpie switched to the Vassal submodule in commit:

`aa30da5d4b0264dbccf11614ae5408702aeae82e`

The parent revision before that conversion is:

`8e095b8f9134cf5498b258ba97069e8f7730c0dd`

## Important limitation

This historical JAR is a **compatibility/archive fallback**, not a claim of byte-for-byte or source-level equivalence to the later unavailable Vassal revision `ea7d583...`. Vassal/Voro++ changed after the direct-JAR period. If a complete authoritative Vassal mirror containing the exact referenced revision is recovered in the future, that source should be preferred and this fallback can be revisited.

The Voro++ helper executables needed by Magpie are already tracked separately in this repository under `exes/`; the Gradle build copies those archived executables into `dist/exes`.
