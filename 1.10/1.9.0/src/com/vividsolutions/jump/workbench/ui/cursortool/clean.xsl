<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    version="1.0">

  <xsl:output method="html" version="3.2" />
  
  <xsl:strip-space elements="*" />

  <xsl:template match="/">
    <xsl:if test="not(html)">
        <html>
            <head>
            </head>
            <body>
                <xsl:apply-templates />
            </body>
        </html>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="*">
	<xsl:choose>
	<xsl:when test="local-name()='img'" />
	<xsl:otherwise>
	    <xsl:element name="{local-name()}">
	      <xsl:apply-templates select="@*|node()"/>
	    </xsl:element>	
	</xsl:otherwise>
	</xsl:choose>
  </xsl:template>
  
  <xsl:template match="@*">
    <xsl:attribute name="{local-name()}">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>
  
</xsl:stylesheet>