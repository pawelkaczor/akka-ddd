import microsites._

lazy val micrositeSettings = Seq(
  micrositeName := "Akka-DDD",
  micrositeDescription := "Scala CQRS/ES framework",
  micrositeBaseUrl := "akka-ddd",
  micrositeDocumentationUrl := "docs/getting-started.html",
  micrositeGithubOwner := "pawelkaczor",
  micrositeGithubRepo := "akka-ddd",
  micrositeAuthor := "Paweł Kaczor",
  micrositeHighlightTheme := "github",
  ghpagesNoJekyll := false,
  micrositeExtraMdFiles := Map(
    file("README.md") -> ExtraMdFileConfig(
      "index.md",
      "home",
      Map("title" -> "Home", "section" -> "home", "position" -> "0")
    )
  ),
  includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.md"
)


// 252338 453F78 759AAB
// 252338 89B6A5 C9EDDC
// Documentation site =======================================
lazy val docs = (project in file("docs"))
  .settings(micrositeSettings: _*)
  .settings(moduleName := "docs")
  .enablePlugins(MicrositesPlugin)
