lazy val micrositeSettings = Seq(
  micrositeName := "Akka-DDD",
  micrositeDescription := "Scala CQRS/ES framework",
  micrositeBaseUrl := "akka-ddd",
  micrositeDocumentationUrl := "/docs/",
  micrositeGithubOwner := "pawelkaczor",
  micrositeGithubRepo := "akka-ddd",
  micrositeAuthor := "Paweł Kaczor",
  micrositeHighlightTheme := "github",
  includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.md"
)


// 252338 453F78 759AAB
// 252338 89B6A5 C9EDDC
// Documentation site =======================================
lazy val docs = (project in file("docs"))
  .settings(micrositeSettings: _*)
  .settings(moduleName := "docs")
  .enablePlugins(MicrositesPlugin)
