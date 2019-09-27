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
	<xsl:call-template name="object"/>
</xsl:template>
<xsl:template name="object">
<!--object-->
	<xsl:value-of select="name(.)" />(オブジェクト):{
	<div class="debug" style="border:1px solid gray">
		<xsl:value-of select="."/>
	</div>
	<ul>
		<li><span class='key'>型</span><span class='value'>=<xsl:value-of select="./type"/></span></li>
		<li><span class='key'>説明</span><span class='value'>=<xsl:value-of select="./title"/>
		<xsl:if test="descrption"><xsl:value-of select="./description"/></xsl:if>
		<br/>
		
		</span></li>
		<xsl:if test="./uniqueItem">
		<li><span class='key'>一意</span><span class='value'><xsl:value-of select="uniqueItem"/></span></li>
		</xsl:if>
		
		<xsl:if test="count(./required)">
		<li><span class='key'>必須プロパティ:</span>
				<xsl:value-of select="./required"/>,
		<xsl:for-each select="./required/*">
			<span class='required'><xsl:value-of select="."/></span>
		</xsl:for-each>
		</li>
		</xsl:if>
		
		<xsl:if test="./items">
			<li><span class='key'>アイテム</span>{<br/>
			<ul>
				<xsl:for-each select="./items/*">
					<!--<li><xsl:call-template name="object"/></li>-->
					<li><xsl:value-of select="."/></li>
					<li><xsl:call-template name="property" /></li>
				</xsl:for-each>
			</ul>
			};
			</li>
		</xsl:if>
		
		<xsl:if test="./properties">				
			<li><span class='key '>プロパティ</span>{
<!--			<div class="debug" style="border:1px solid blue"><xsl:value-of select="name(.)"/>:{<xsl:value-of select="."/>}</div>-->

				<ul>
				<xsl:for-each select="./properties/*">
					<div class="debug" style="border:1px solid "><xsl:value-of select="name(.)"/>:{<xsl:value-of select="."/>}</div>
					<li><xsl:call-template name="property"/></li>
				</xsl:for-each>
				}
				</ul>

			</li>

		</xsl:if>
	</ul>
	}
</xsl:template>

<xsl:template name="property">
		<xsl:value-of select="name(.)"/>(p):
			
<!--		<xsl:variable name="type"><xsl:value-of select="./type"/></xsl:variable>-->
		<xsl:choose>
			<xsl:when test="./type='object'">オブジェクト<br/>
				<xsl:call-template name="object"/>
			</xsl:when>
			<xsl:when test="./type='array'">配列[<br/>
				<xsl:apply-templates select="./items"/>
				]<br/>
			</xsl:when>
			<xsl:when test="./type='string'">				文字列<br/></xsl:when>

			<xsl:when test="./type='number'">				数値<br/></xsl:when>

			<xsl:when test="./type='Date'">日付</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="./type"/><br/>
			</xsl:otherwise>
		</xsl:choose>
		<ul>
		<li><span class='key'>説明</span><span class='value'><xsl:value-of select="title"/>
		<xsl:if test="./descrption"><xsl:value-of select="./description"/></xsl:if>
		<br/>
		</span></li>

		<xsl:if test="uniqueItem">
		<li><span class='key'>一意</span><span class='value'><xsl:value-of select="uniqueItem"/></span></li>
		</xsl:if>
		
		<xsl:if test="count(./required)">
		<li><span class='key'>必須プロパティ:</span>
				<xsl:value-of select="./required"/>,
		<xsl:for-each select="./required/*">
			<span class='required'><xsl:value-of select="."/></span>
		</xsl:for-each>
		</li>
		</xsl:if>
		</ul>

		
</xsl:template>
</xsl:stylesheet>
