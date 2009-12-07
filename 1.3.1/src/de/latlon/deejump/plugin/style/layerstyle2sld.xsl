<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:deegreewfs="http://www.deegree.org/wfs" xmlns:java="java" xmlns:xslutil="de.latlon.deejump.plugin.style.XSLUtility" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" version="1.0">
  <xsl:param name="defaultFillColor" select="xslutil:toHexColor(/layer/styles/style[1]/fill/color)"/>
  <xsl:param name="defaultStrokeColor" select="xslutil:toHexColor(/layer/styles/style[1]/line/color)"/>
  <xsl:param name="defaultStrokeWidth" select="/layer/styles/style[1]/line/@width"/>
  <xsl:param name="wmsLayerName" select="/layer/@wmsLayerName"/>
  <xsl:param name="featureTypeStyle" select="/layer/@featureTypeStyle"/>
  <xsl:param name="styleName" select="/layer/@styleName"/>
  <xsl:param name="styleTitle" select="/layer/@styleTitle"/>
  <xsl:param name="geomProperty">GEOM</xsl:param>
  <xsl:param name="geoType" select="/layer/@geoType"/>
  <xsl:param name="minScale">0</xsl:param>
  <xsl:param name="maxScale">999999999999</xsl:param>
  <xsl:param name="Namespace"/>
  <xsl:param name="NamespacePrefixWithoutColon"/>
  <xsl:param name="NamespacePrefix"/>

  <xsl:template match="/">
    <sld:StyledLayerDescriptor xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:gml="http://www.opengis.net/gml" xmlns:wfs="http://www.opengis.net/wfs" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.0.0">
		<xsl:attribute name="{ $NamespacePrefixWithoutColon }:dummy" namespace="{ $Namespace }"/>
      <xsl:apply-templates select="layer"/>
    </sld:StyledLayerDescriptor>
  </xsl:template>
  <xsl:template match="layer">
    <sld:NamedLayer>
      <sld:Name>
        <xsl:value-of select="$wmsLayerName"/>
      </sld:Name>
      <sld:UserStyle>
        <sld:Name>
          <xsl:value-of select="$styleName"/>
        </sld:Name>
        <sld:Title>
          <xsl:value-of select="$styleTitle"/>
        </sld:Title>
        <sld:IsDefault>1</sld:IsDefault>
        <sld:FeatureTypeStyle>
          <sld:Name>
            <xsl:value-of select="$featureTypeStyle"/>
          </sld:Name>
          <xsl:apply-templates select="styles/style"/>
        </sld:FeatureTypeStyle>
      </sld:UserStyle>
    </sld:NamedLayer>
  </xsl:template>

  <!-- template for theming styles -->
  <xsl:template match="style">
    <xsl:if test="@class='com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle'">
      <xsl:if test="@enabled='true'">
        <xsl:choose>
          <xsl:when test="contains($geoType,'Polygon')">
            <sld:Rule>
              <ogc:Filter>
                <ogc:PropertyIsInstanceOf>
                  <ogc:PropertyName>app:geometry</ogc:PropertyName>
                  <ogc:Literal>gml:_Surface</ogc:Literal>
                </ogc:PropertyIsInstanceOf>
              </ogc:Filter>
              <sld:Name>basicPolyStyle</sld:Name>
              <sld:MinScaleDenominator>
                <xsl:value-of select="$minScale"/>
              </sld:MinScaleDenominator>
              <sld:MaxScaleDenominator>
                <xsl:value-of select="$maxScale"/>
              </sld:MaxScaleDenominator>
              <sld:PolygonSymbolizer>
                <sld:Geometry>
                  <ogc:PropertyName><xsl:value-of select="$NamespacePrefix"/><xsl:value-of select="$geomProperty"/></ogc:PropertyName>
                </sld:Geometry>
                <xsl:apply-templates select="fill"/>
                <xsl:apply-templates select="line"/>
              </sld:PolygonSymbolizer>
            </sld:Rule>
          </xsl:when>
          <xsl:when test="contains($geoType,'Line')">
            <sld:Rule>
              <ogc:Filter>
                <ogc:PropertyIsInstanceOf>
                  <ogc:PropertyName>app:geometry</ogc:PropertyName>
                  <ogc:Literal>gml:_Curve</ogc:Literal>
                </ogc:PropertyIsInstanceOf>
              </ogc:Filter>
              <sld:Name>basicLineStyle</sld:Name>
              <sld:MinScaleDenominator>
                <xsl:value-of select="$minScale"/>
              </sld:MinScaleDenominator>
              <sld:MaxScaleDenominator>
                <xsl:value-of select="$maxScale"/>
              </sld:MaxScaleDenominator>
              <sld:LineSymbolizer>
                <sld:Geometry>
                  <ogc:PropertyName><xsl:value-of select="$NamespacePrefix"/><xsl:value-of select="$geomProperty"/></ogc:PropertyName>
                </sld:Geometry>
                <xsl:apply-templates select="line"/>
              </sld:LineSymbolizer>
            </sld:Rule>
          </xsl:when>
          <xsl:when test="contains($geoType,'Point')">
            <sld:Rule>
              <ogc:Filter>
                <ogc:PropertyIsInstanceOf>
                  <ogc:PropertyName>app:geometry</ogc:PropertyName>
                  <ogc:Literal>gml:Point</ogc:Literal>
                </ogc:PropertyIsInstanceOf>
              </ogc:Filter>
              <sld:Name>basicPointStyle</sld:Name>
              <sld:MinScaleDenominator>
                <xsl:value-of select="$minScale"/>
              </sld:MinScaleDenominator>
              <sld:MaxScaleDenominator>
                <xsl:value-of select="$maxScale"/>
              </sld:MaxScaleDenominator>
              <sld:PointSymbolizer>
                <sld:Geometry>
                  <ogc:PropertyName><xsl:value-of select="$NamespacePrefix"/><xsl:value-of select="$geomProperty"/></ogc:PropertyName>
                </sld:Geometry>
                <sld:Graphic>
                  <sld:Mark>
                    <xsl:apply-templates select="fill"/>
                    <xsl:apply-templates select="line"/>
                  </sld:Mark>
                </sld:Graphic>
              </sld:PointSymbolizer>
            </sld:Rule>
          </xsl:when>
          <xsl:otherwise>
            <!-- Something that's not implemented -->
          </xsl:otherwise>
        </xsl:choose>
      </xsl:if>
    </xsl:if>
    <!-- normal color theming style -->
    <xsl:if test="@class='com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle'">
      <xsl:if test="@enabled='true'">
        <xsl:apply-templates select="attribute-value-to-style-map"/>
      </xsl:if>
    </xsl:if>
    <!-- label style -->
    <xsl:if test="@class='com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle'">
      <xsl:if test="@enabled='true'">
        <sld:Rule>
          <sld:Name>labelStyle</sld:Name>
          <sld:MinScaleDenominator>
            <xsl:value-of select="$minScale"/>
          </sld:MinScaleDenominator>
          <sld:MaxScaleDenominator>
            <xsl:value-of select="$maxScale"/>
          </sld:MaxScaleDenominator>
          <sld:TextSymbolizer>
            <sld:Geometry>
              <ogc:PropertyName><xsl:value-of select="$NamespacePrefix"/><xsl:value-of select="$geomProperty"/></ogc:PropertyName>
            </sld:Geometry>
            <sld:Label>
              <ogc:PropertyName><xsl:value-of select="$NamespacePrefix"/><xsl:value-of select="./attribute"/></ogc:PropertyName>
            </sld:Label>
            <sld:Font>
              <sld:CssParameter name="font-family">
                <xsl:value-of select="xslutil:toFontFamily(font)"/>
              </sld:CssParameter>
              <sld:CssParameter name="font-style">
                <xsl:value-of select="xslutil:toFontStyle(font)"/>
              </sld:CssParameter>
              <sld:CssParameter name="font-size">
                <xsl:value-of select="./height"/>
              </sld:CssParameter>
              <sld:CssParameter name="font-color"> <!-- not in the SLD standard,
                WMS should use fill! -->
                <xsl:value-of select="xslutil:toHexColor(color)"/>
              </sld:CssParameter>
            </sld:Font>
            <xsl:if test="not(angleAttribute='')">
              <sld:LabelPlacement>
                <sld:PointPlacement>
                  <sld:Rotation>
                    <ogc:Mul>
                      <ogc:Literal>-1</ogc:Literal>
                      <ogc:PropertyName><xsl:value-of select="$NamespacePrefix"/><xsl:value-of select="angleAttribute"/></ogc:PropertyName>
                    </ogc:Mul>
                  </sld:Rotation>
                </sld:PointPlacement>
              </sld:LabelPlacement>
            </xsl:if>
            <xsl:if test="outlineShowing='true'">
              <sld:Halo>
                <sld:Radius>
                  <xsl:value-of select="outlineWidth" />
                </sld:Radius>
                <sld:Fill>
                  <sld:CssParameter name="fill">
                    <xsl:value-of select="xslutil:toHexColor(outlineColor)" />
                  </sld:CssParameter>
                </sld:Fill>
              </sld:Halo>
            </xsl:if>
            <sld:Fill>
              <sld:CssParameter name="fill">
                <xsl:value-of select="xslutil:toHexColor(color)"/>
              </sld:CssParameter>
              <sld:CssParameter name="fill-opacity">
                <xsl:value-of select="xslutil:toAlphaValue(alpha)"/>
              </sld:CssParameter>
            </sld:Fill>
          </sld:TextSymbolizer>
        </sld:Rule>
      </xsl:if>
    </xsl:if>
    <xsl:if test="contains(@class, 'VertexStyle')">
      <xsl:if test="@enabled='true'">
        <sld:Rule>
          <sld:Name>pointStyle</sld:Name>
          <sld:MinScaleDenominator>
            <xsl:value-of select="$minScale"/>
          </sld:MinScaleDenominator>
          <sld:MaxScaleDenominator>
            <xsl:value-of select="$maxScale"/>
          </sld:MaxScaleDenominator>
          <sld:PointSymbolizer>
            <sld:Geometry>
              <ogc:PropertyName><xsl:value-of select="$NamespacePrefix"/><xsl:value-of select="$geomProperty"/></ogc:PropertyName>
            </sld:Geometry>
            <sld:Graphic>
              <xsl:choose>
                <xsl:when test="contains(@class, 'BitmapVertexStyle')">
                  <sld:ExternalGraphic>
                    <OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="simple">
                      <xsl:attribute name="xlink:href">
                        <xsl:value-of select="xslutil:getImageURL(@imageURL, ../style[contains(@class, 'BasicStyle')]/fill/color, ../style[contains(@class, 'BasicStyle')]/line/color, number(@size))"/>
                      </xsl:attribute>
                    </OnlineResource>
                    <sld:Format>
                      <xsl:choose>
                        <xsl:when test="contains(@imageURL, 'png')">image/png</xsl:when>
                        <xsl:when test="contains(@imageURL, 'jpg')">image/jpg</xsl:when>
                        <xsl:when test="contains(@imageURL, 'gif')">image/gif</xsl:when>
                        <xsl:when test="contains(@imageURL, 'svg')">image/png</xsl:when>
                        <xsl:otherwise>unknown format</xsl:otherwise>
                      </xsl:choose>
                    </sld:Format>
                  </sld:ExternalGraphic>
                </xsl:when>
                <xsl:otherwise>
                  <sld:Mark>
                    <sld:WellKnownName>
                      <xsl:value-of select="xslutil:toWellKnowName(.)"/>
                    </sld:WellKnownName>
                    <xsl:apply-templates select="../style[contains(@class, 'BasicStyle')]/fill" />
                    <xsl:apply-templates select="../style[contains(@class, 'BasicStyle')]/line" />
                  </sld:Mark>
                </xsl:otherwise>
              </xsl:choose>
              <xsl:if test="string-length(@size) &gt; 0">
                <sld:Size>
                  <xsl:choose>
                    <xsl:when test="contains(@imageURL, 'svg')"><xsl:value-of select="@size" /></xsl:when>
                    <xsl:otherwise>
                      <xsl:value-of select="@size * 2"/>
                    </xsl:otherwise>
                  </xsl:choose>
                </sld:Size>
              </xsl:if>
            </sld:Graphic>
          </sld:PointSymbolizer>
        </sld:Rule>
      </xsl:if>
    </xsl:if>
  </xsl:template>

  <xsl:template match="attribute-value-to-style-map/mapping" name="rules">
    <sld:Rule>
      <xsl:if test="../@class='java.util.TreeMap'">
        <sld:Name><xsl:value-of select="./key"/></sld:Name>
        <ogc:Filter>
          <ogc:PropertyIsLike wildCard="*" singleChar="?" escape="\">
            <ogc:PropertyName><xsl:value-of select="$NamespacePrefix"/><xsl:value-of select="../../attribute-name"/></ogc:PropertyName>
            <ogc:Literal>
              <xsl:value-of select="./key"/>
            </ogc:Literal>
          </ogc:PropertyIsLike>
        </ogc:Filter>
      </xsl:if>
      <xsl:if test="../@class='com.vividsolutions.jump.util.Range$RangeTreeMap'">
        <sld:Name><xsl:value-of select="./key/min"/> - <xsl:value-of select="./key/max"/></sld:Name>
        <ogc:Filter>
          <ogc:PropertyIsBetween>
            <ogc:PropertyName><xsl:value-of select="$NamespacePrefix"/><xsl:value-of select="../../attribute-name"/></ogc:PropertyName>
            <ogc:LowerBoundary>
              <ogc:Literal>
                <xsl:value-of select="./key/min"/>
              </ogc:Literal>
            </ogc:LowerBoundary>
            <ogc:UpperBoundary>
              <ogc:Literal>
                <xsl:choose>
                  <xsl:when test="./key/max/@class='com.vividsolutions.jump.util.Range$PositiveInfinity'">999999999</xsl:when>
                  <xsl:when test="./key/max/@class='com.vividsolutions.jump.util.Range$NegativeInfinity'">-999999999</xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="./key/max"/>
                  </xsl:otherwise>
                </xsl:choose>
              </ogc:Literal>
            </ogc:UpperBoundary>
          </ogc:PropertyIsBetween>
        </ogc:Filter>
      </xsl:if>
      <sld:MinScaleDenominator>
        <xsl:value-of select="$minScale"/>
      </sld:MinScaleDenominator>
      <sld:MaxScaleDenominator>
        <xsl:value-of select="$maxScale"/>
      </sld:MaxScaleDenominator>
      <xsl:choose>
        <xsl:when test="contains($geoType,'Polygon')">
          <sld:PolygonSymbolizer>
            <sld:Geometry>
              <ogc:PropertyName><xsl:value-of select="$NamespacePrefix"/><xsl:value-of select="$geomProperty"/></ogc:PropertyName>
            </sld:Geometry>
            <xsl:apply-templates select="value/fill"/>
            <xsl:apply-templates select="value/line"/>
          </sld:PolygonSymbolizer>
        </xsl:when>
        <xsl:when test="contains($geoType,'Line')">
          <sld:LineSymbolizer>
            <sld:Geometry>
              <ogc:PropertyName><xsl:value-of select="$NamespacePrefix"/><xsl:value-of select="$geomProperty"/></ogc:PropertyName>
            </sld:Geometry>
            <xsl:apply-templates select="value/line"/>
          </sld:LineSymbolizer>
        </xsl:when>
        <xsl:when test="contains($geoType,'Point')">
          <sld:PointSymbolizer>
            <sld:Geometry>
              <ogc:PropertyName><xsl:value-of select="$NamespacePrefix"/><xsl:value-of select="$geomProperty"/></ogc:PropertyName>
            </sld:Geometry>
            <sld:Graphic>
              <xsl:choose>
                <xsl:when test="contains(./value/vertexstyle/@class, 'BitmapVertexStyle')">
                  <sld:ExternalGraphic>
                    <OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="simple">
                      <xsl:attribute name="xlink:href">
                        <xsl:value-of select="xslutil:getImageURL(./value/vertexstyle/@imageURL, value/fill/color, value/fill/stroke-color, number(value/vertexstyle/@size))"/>
                      </xsl:attribute>
                    </OnlineResource>
                    <sld:Format>
                      <xsl:choose>
                        <xsl:when test="contains(value/vertexstyle/@imageURL, 'png')">image/png</xsl:when>
                        <xsl:when test="contains(value/vertexstyle/@imageURL, 'jpg')">image/jpg</xsl:when>
                        <xsl:when test="contains(value/vertexstyle/@imageURL, 'gif')">image/gif</xsl:when>
                        <xsl:when test="contains(value/vertexstyle/@imageURL, 'svg')">image/png</xsl:when>
                        <xsl:otherwise>unknown format</xsl:otherwise>
                      </xsl:choose>
                    </sld:Format>
                  </sld:ExternalGraphic>
                  <sld:Mark/>
                </xsl:when>
                <xsl:otherwise>
                  <sld:Mark>
                    <sld:WellKnownName>
                      <xsl:value-of select="xslutil:toWellKnowName(./value/vertexstyle)"/>
                    </sld:WellKnownName>
                    <sld:Fill>
                      <!-- fill is the color of basic style -->
                      <sld:CssParameter name="fill">
                        <xsl:value-of select="xslutil:toHexColor(value/fill/color)"/>
                      </sld:CssParameter>
                    </sld:Fill>
                    <sld:Stroke>
                      <sld:CssParameter name="stroke">
                        <xsl:value-of select="xslutil:toHexColor(value/line/stroke-color)"/>
                      </sld:CssParameter>
                    </sld:Stroke>
                  </sld:Mark>
                </xsl:otherwise>
              </xsl:choose>
              <xsl:if test="string-length(value/vertexstyle/@size) &gt; 0">
                <sld:Size>
                  <xsl:value-of select="value/vertexstyle/@size"/>
                </sld:Size>
              </xsl:if>
            </sld:Graphic>
          </sld:PointSymbolizer>
        </xsl:when>
      </xsl:choose>
    </sld:Rule>
  </xsl:template>

  <xsl:template match="fill">
    <xsl:if test="@enabled='true'">
      <sld:Fill>
        <xsl:choose>
          <xsl:when test="pattern/@class='com.vividsolutions.jump.workbench.ui.renderer.style.ImageFillPattern' and pattern/@enabled='true'">
            <xsl:variable name="imageURL">
              <xsl:value-of select="xslutil:getIconURL(pattern/properties/properties/mapping[string(key) = 'FILENAME']/value)"/>
            </xsl:variable>
            <xsl:variable name="fileName">
              <xsl:value-of select="translate(string(pattern/properties/properties/mapping[string(key) = 'FILENAME']/value), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')"/>
            </xsl:variable>
            <sld:GraphicFill>
              <sld:Graphic>
                <sld:ExternalGraphic>
                  <OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="simple">
                    <xsl:attribute name="xlink:href">
                      <xsl:value-of select="$imageURL"/>
                    </xsl:attribute>
                  </OnlineResource>
                  <sld:Format>
                    <xsl:choose>
                      <xsl:when test="contains($fileName, 'png')">image/png</xsl:when>
                      <xsl:when test="contains($fileName, 'jpg')">image/jpg</xsl:when>
                      <xsl:when test="contains($fileName, 'gif')">image/gif</xsl:when>
                      <xsl:when test="contains($fileName, 'svg')">image/svg+xml</xsl:when>
                      <xsl:otherwise>unknown format</xsl:otherwise>
                    </xsl:choose>
                  </sld:Format>
                </sld:ExternalGraphic>
              </sld:Graphic>
            </sld:GraphicFill>
          </xsl:when>
          <xsl:when test="pattern/@class='com.vividsolutions.jump.workbench.ui.renderer.style.WKTFillPattern' and pattern/@enabled='true'">
            <xsl:variable name="width">
              <xsl:value-of select="number(pattern/properties/properties/mapping[string(key) = 'LINE WIDTH']/value)"/>
            </xsl:variable>
            <xsl:variable name="extent">
              <xsl:value-of select="number(pattern/properties/properties/mapping[string(key) = 'EXTENT']/value)"/>
            </xsl:variable>
            <xsl:variable name="pattern">
              <xsl:value-of select="string(pattern/properties/properties/mapping[string(key) = 'PATTERN WKT']/value)"/>
            </xsl:variable>
            <xsl:variable name="color">
              <xsl:value-of select="xslutil:toHexColor(pattern/properties/properties/mapping[string(key) = 'COLOR']/value)"/>
            </xsl:variable>
            <sld:GraphicFill>
              <sld:Graphic>
                <sld:ExternalGraphic>
                  <OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="simple">
                    <xsl:attribute name="xlink:href">
                      <xsl:value-of select="xslutil:createPatternImage(number($width), number($extent), $pattern, $color)"/>
                    </xsl:attribute>
                  </OnlineResource>
                  <sld:Format>image/png</sld:Format>
                </sld:ExternalGraphic>
              </sld:Graphic>
            </sld:GraphicFill>
          </xsl:when>
          <xsl:when test="pattern/@class='org.openjump.util.CustomTexturePaint' and pattern/@enabled='true'">
            <sld:GraphicFill>
              <sld:Graphic>
                <sld:ExternalGraphic>
                  <OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="simple">
                    <xsl:attribute name="xlink:href">
                      <xsl:value-of select="pattern[@class='org.openjump.util.CustomTexturePaint']/url"/>
                    </xsl:attribute>
                  </OnlineResource>
                  <sld:Format>image/png</sld:Format>
                </sld:ExternalGraphic>
              </sld:Graphic>
            </sld:GraphicFill>
          </xsl:when>
          <xsl:otherwise>
            <sld:CssParameter name="fill">
              <xsl:value-of select="xslutil:toHexColor(color)"/>
            </sld:CssParameter>
            <sld:CssParameter name="fill-opacity">
              <xsl:value-of select="xslutil:toAlphaValue(../alpha)"/>
            </sld:CssParameter>
          </xsl:otherwise>
        </xsl:choose>
      </sld:Fill>
    </xsl:if>
  </xsl:template>

  <xsl:template match="line">
    <xsl:if test="@enabled='true'">
      <sld:Stroke>
        <sld:CssParameter name="stroke">
          <xsl:value-of select="xslutil:toHexColor(color)"/>
        </sld:CssParameter>
        <sld:CssParameter name="stroke-opacity">
          <xsl:value-of select="xslutil:toAlphaValue(../alpha)"/>
        </sld:CssParameter>
        <sld:CssParameter name="stroke-width">
          <xsl:value-of select="@width"/>
        </sld:CssParameter>
        <xsl:if test="pattern[@enabled='true']">
          <sld:CssParameter name="stroke-dasharray">
            <xsl:value-of select="xslutil:replaceComma(pattern)"/>
          </sld:CssParameter>
        </xsl:if>
      </sld:Stroke>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
