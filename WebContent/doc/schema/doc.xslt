<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema">
   
 <xsl:output method="html" encoding="utf-8" doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  indent="yes" omit-xml-declaration="no" />

<xsl:template match="/">
<html xmlns="http://www.w3.org/1999/xhtml" lang="ja-JP" xml:lang="ja-JP">
<head>
<title>test</title>
<style type = 'text/css'>
.key{
	display: inline-block;
	min-length:10em;
	max-length:10em;
	width:10em;
}
.value{
	display: inline-block;
	white-space:pre-wrap;
}
.debug{
display:none;
}
</style>
</head>
<body>
<h2>test</h2>
<xsl:message>root</xsl:message>
<!--<xsl:call-template name="object" />-->
<h3><xsl:value-of select="./schema/@name"/></h3>
<xsl:apply-templates select="./schema" />

</body>
</html>

</xsl:template>


<xsl:template match="schema" >
	aaaa
</xsl:template>

<xsl:template match="properties">
prop:
</xsl:template>

<xsl:template match="items">
item:
</xsl:template>
		
</xsl:stylesheet>
