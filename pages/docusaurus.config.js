// The sources are the Markdown files under `docs/` at the root of the repository, but Docusaurus reads them from
// `target/mdoc`, where `sbt pages/mdoc` leaves them with every `scala mdoc` block compiled and evaluated.
module.exports = {
  title: 'Stages',
  url: 'https://stages.h8.io',
  baseUrl: '/',
  onBrokenLinks: 'throw',
  onBrokenAnchors: 'throw',
  markdown: {
    // CommonMark rather than MDX: braces and angle brackets in prose are Scala, not JSX.
    format: 'detect',
    hooks: { onBrokenMarkdownLinks: 'throw' },
  },
  presets: [
    [
      'classic',
      {
        docs: { path: 'target/mdoc', routeBasePath: '/', sidebarPath: require.resolve('./sidebars.js') },
        blog: false,
        pages: false,
        theme: {},
      },
    ],
  ],
  themeConfig: {
    navbar: {
      title: 'Stages',
      items: [
        // `pathname://` because the API is copied next to the site after it is built, out of the link checker's sight.
        { href: 'pathname:///api/scala-2.13/', label: 'API', position: 'right' },
        { href: 'https://github.com/h8io/stages', label: 'GitHub', position: 'right' },
      ],
    },
    // Prism's Scala grammar extends its Java one, which is not loaded by default either.
    prism: { additionalLanguages: ['java', 'scala'] },
  },
};
