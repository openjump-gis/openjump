<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:deegreewfs="http://www.deegree.org/wfs" xmlns:java="java" xmlns:xslutil="de.latlon.deejump.plugin.style.XSLUtility" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" >

	<xsl:param name="defaultFillColor" select="xslutil:toHexColor(/layer/styles/style[1]/fill/color)"/>
	<xsl:param name="defaultStrokeColor" select="xslutil:toHexColor(/layer/styles/style[1]/line/color)"/>
	<xsl:param name="defaultStrokeWidth" select="/layer/styles/style[1]/line/@width"/>
	<xsl:param name="wmsLayerName" select="/layer/@wmsLayerName"/>
	<xsl:param name="featureTypeStyle" select="/layer/@featureTypeStyle"/>
	<xsl:param name="styleName" select="/layer/@styleName"/>
	<xsl:param name="styleTitle" select="/layer/@styleTitle"/>
	<xsl:param name="geomProperty" >GEOM</xsl:param>
	<xsl:param name="geoType" select="/layer/@geoType"/>
	<xsl:param name="minScale">0</xsl:param>
	<xsl:param name="maxScale">999999999999</xsl:param>

	<xsl:output method="XML" encoding="UTF-8"/>

	<xsl:template match="/">
		<sld:StyledLayerDescriptor version="1.0.0"
			xmlns="http://www.opengis.net/sld"
			xmlns:sld="http://www.opengis.net/sld"
			xmlns:gml="http://www.opengis.net/gml"
			xmlns:wfs="http://www.opengis.net/wfs"
			xmlns:ogc="http://www.opengis.net/ogc"
			xmlns:xlink="http://www.w3.org/1999/xlink"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xmlns:app="http://www.deegree.org/app">
			<xsl:apply-templates select="./layer" />
			<!-- sometimes it is "project" here -->
		</sld:StyledLayerDescriptor>
	</xsl:template>
	<!-- template for all layers -->
	<xsl:template match="layer" name="layer">
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
					<xsl:apply-templates select="./styles/style"/>
				</sld:FeatureTypeStyle>
			</sld:UserStyle>
		</sld:NamedLayer>
	</xsl:template>
	<!-- template for theming styles -->
	<xsl:template match="style" name="basicstyle">
		<xsl:if test="@class='com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle'">
			<xsl:if test="@enabled='true'">			
				<xsl:choose>
					<xsl:when test="contains($geoType,'Polygon')">
						<sld:Rule>
							<sld:Name>basicPolyStyle</sld:Name>
							<sld:MinScaleDenominator><xsl:value-of select="$minScale"/></sld:MinScaleDenominator>
							<sld:MaxScaleDenominator><xsl:value-of select="$maxScale"/></sld:MaxScaleDenominator>
							<sld:PolygonSymbolizer>
								<sld:Geometry>
									<ogc:PropertyName><xsl:value-of select="$geomProperty"/></ogc:PropertyName>
								</sld:Geometry>
								<xsl:apply-templates select="./fill"/>
								<xsl:apply-templates select="./line"/>
							</sld:PolygonSymbolizer>
						</sld:Rule>
					</xsl:when>
					<xsl:when test="contains($geoType,'Line')">
						<sld:Rule>
							<sld:Name>basicLineStyle</sld:Name>
							<sld:MinScaleDenominator><xsl:value-of select="$minScale"/></sld:MinScaleDenominator>
							<sld:MaxScaleDenominator><xsl:value-of select="$maxScale"/></sld:MaxScaleDenominator>
							<sld:LineSymbolizer>
								<sld:Geometry>
									<ogc:PropertyName><xsl:value-of select="$geomProperty"/></ogc:PropertyName>
								</sld:Geometry>
								<sld:Stroke>
									<sld:CssParameter name="stroke">
										<xsl:value-of select="$defaultStrokeColor"/>
									</sld:CssParameter>
									<sld:CssParameter name="stroke-width">
										<xsl:value-of select="$defaultStrokeWidth"/>
									</sld:CssParameter>
								</sld:Stroke>
							</sld:LineSymbolizer>
						</sld:Rule>
					</xsl:when>
					<xsl:otherwise>

					</xsl:otherwise>
				</xsl:choose>
			</xsl:if>
		</xsl:if> <!-- normal color theming style -->
		<xsl:if test="@class='com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle'">
			<xsl:if test="@enabled='true'">
				<xsl:apply-templates select="./attribute-value-to-style-map"/>
			</xsl:if>
		</xsl:if><!-- normal deeJUMP color theming style for points -->
		<xsl:if test="@class='de.latlon.deejump.plugin.style.DeeColorThemingStyle'">
			<xsl:if test="@enabled='true'">
				<xsl:apply-templates select="./attribute-value-to-style-map"/>
			</xsl:if>
		</xsl:if>
		<!-- label style -->
		<xsl:if test="@class='com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle'">
			<xsl:if test="@enabled='true'">
				<sld:Rule>
					<sld:Name>labelStyle</sld:Name>
					<sld:MinScaleDenominator><xsl:value-of select="$minScale"/></sld:MinScaleDenominator>
					<sld:MaxScaleDenominator><xsl:value-of select="$maxScale"/></sld:MaxScaleDenominator>
						<sld:TextSymbolizer>
						<sld:Geometry>
							<ogc:PropertyName><xsl:value-of select="$geomProperty"/></ogc:PropertyName>
						</sld:Geometry>
						<sld:Label>
							<ogc:PropertyName>
								<xsl:value-of select="./attribute"/>
							</ogc:PropertyName>
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
						</sld:Font>
						<sld:Fill>
							<sld:CssParameter name="fill">
								<xsl:value-of select="xslutil:toHexColor(color)"/>
							</sld:CssParameter>
						</sld:Fill>
						<sld:Halo/>
					</sld:TextSymbolizer>
				</sld:Rule>
			</xsl:if>
		</xsl:if>
		<xsl:if test="contains(@class, 'VertexStyle')">
			<xsl:if test="@enabled='true'">
				<sld:Rule>
					<sld:Name>pointStyle</sld:Name>
					<sld:MinScaleDenominator><xsl:value-of select="$minScale"/></sld:MinScaleDenominator>
					<sld:MaxScaleDenominator><xsl:value-of select="$maxScale"/></sld:MaxScaleDenominator>
					<sld:PointSymbolizer>
						<sld:Geometry>
							<ogc:PropertyName><xsl:value-of select="$geomProperty"/></ogc:PropertyName>
						</sld:Geometry>
						<sld:Graphic>
							<xsl:choose>
								<xsl:when test="contains(@class, 'BitmapVertexStyle')">
									<sld:ExternalGraphic>
										<OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="simple">
											<xsl:attribute name="xlink:href"><xsl:value-of select="xslutil:fileToURL(@imageURL)"/></xsl:attribute>
										</OnlineResource>
										<sld:Format>
											<xsl:choose>
												<!-- does this exist? 
										<xsl:when test="ends-with((@imageURL, 'png')"> 
										or perhaps use "image/" + substring( imgname, end - 3 , till end) that 'd return the extension
										-->
												<xsl:when test="contains(@imageURL, 'png')">image/png</xsl:when>
												<xsl:when test="contains(@imageURL, 'jpg')">image/jpg</xsl:when>
												<xsl:when test="contains(@imageURL, 'gif')">image/gif</xsl:when>
												<xsl:otherwise>unknown format</xsl:otherwise>
											</xsl:choose>
										</sld:Format>
									</sld:ExternalGraphic>
									<sld:Mark/>
								</xsl:when>
								<xsl:otherwise>
									<sld:Mark>
										<sld:WellKnownName>
											<xsl:value-of select="xslutil:toWellKnowName(.)"/>
										</sld:WellKnownName>
										<sld:Fill>
											<!-- fill is the color of basic style -->
											<sld:CssParameter name="fill">
												<xsl:value-of select="$defaultFillColor"/>
											</sld:CssParameter>
										</sld:Fill>
									</sld:Mark>
									<sld:Size>
										<xsl:value-of select="@size" />
									</sld:Size>
								</xsl:otherwise>
							</xsl:choose>
						</sld:Graphic>
					</sld:PointSymbolizer>
				</sld:Rule>
			</xsl:if>
		</xsl:if>
	</xsl:template>
	<!-- FIXME hmm don't like it. shouldn't go so deep here; should go attribute-value-to-style-map first -->
	<xsl:template match="attribute-value-to-style-map/mapping" name="rules">
		<sld:Rule>
			<sld:Name>
				<xsl:value-of select="../../attribute-name"/>_<xsl:value-of select="./key"/>
			</sld:Name>
			<sld:MinScaleDenominator><xsl:value-of select="$minScale"/></sld:MinScaleDenominator>
			<sld:MaxScaleDenominator><xsl:value-of select="$maxScale"/></sld:MaxScaleDenominator>
								<xsl:if test="../@class='java.util.TreeMap'">
				<ogc:Filter>
					<ogc:PropertyIsLike wildCard="*" singleChar="?" escape="\">
						<ogc:PropertyName>
							<xsl:value-of select="../../attribute-name"/>
						</ogc:PropertyName>
						<ogc:Literal>
							<xsl:value-of select="./key"/>
						</ogc:Literal>
					</ogc:PropertyIsLike>
				</ogc:Filter>
			</xsl:if>
			<xsl:if test="../@class='com.vividsolutions.jump.util.Range$RangeTreeMap'">
				<ogc:Filter>
					<ogc:PropertyIsBetween>
						<ogc:PropertyName>
							<xsl:value-of select="../../attribute-name"/>
						</ogc:PropertyName>
						<ogc:LowerBoundary>
							<ogc:Literal>
								<xsl:value-of select="./key/min"/>
							</ogc:Literal>
						</ogc:LowerBoundary>
						<ogc:UpperBoundary>
							<ogc:Literal>
								<xsl:choose>
									<xsl:when test="./key/max/@class='com.vividsolutions.jump.util.Range$PositiveInfinity'">
								999999999
								</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="./key/max"/>
									</xsl:otherwise>
								</xsl:choose>
							</ogc:Literal>
						</ogc:UpperBoundary>
					</ogc:PropertyIsBetween>
				</ogc:Filter>
			</xsl:if>
			<xsl:choose>
				<xsl:when test="contains($geoType,'Polygon')">
					<sld:PolygonSymbolizer>
						<sld:Geometry>
							<ogc:PropertyName><xsl:value-of select="$geomProperty"/></ogc:PropertyName>
							<!-- [PENDING: this is actually user input; but for shapes almost always GEOM ] -->
						</sld:Geometry>
						<xsl:apply-templates select="./value/fill"/>
						<xsl:apply-templates select="./value/line"/>
					</sld:PolygonSymbolizer>
				</xsl:when>
				<xsl:when test="contains($geoType,'Line')">
					<sld:LineSymbolizer>
						<sld:Geometry>
							<ogc:PropertyName>GEOM</ogc:PropertyName>
							<!-- [PENDING: this is actually user input; but for shapes almost always GEOM ] -->
						</sld:Geometry>
						<xsl:apply-templates select="./value/line"/>
					</sld:LineSymbolizer>
				</xsl:when>				
				<xsl:when test="contains($geoType,'Point')">
					<sld:PointSymbolizer>
						<sld:Geometry>
							<ogc:PropertyName>GEOM</ogc:PropertyName>
						</sld:Geometry>
						<sld:Graphic>
							<xsl:choose>
								<xsl:when test="contains(./value/vertexstyle/@class, 'BitmapVertexStyle')">
									<sld:ExternalGraphic>
										<OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="simple">
											<xsl:attribute name="xlink:href"><xsl:value-of select="xslutil:fileToURL(./value/vertexstyle/@imageURL)"/></xsl:attribute>
										</OnlineResource>
										<sld:Format>
											<xsl:choose>
												<!-- does this exist? 
										<xsl:when test="ends-with((@imageURL, 'png')"> 
										or perhaps use "image/" + substring( imgname, end - 3 , till end) that 'd return the extension
										-->
												<xsl:when test="contains(./value/vertexstyle/@imageURL, 'png')">image/png</xsl:when>
												<xsl:when test="contains(./value/vertexstyle/@imageURL, 'jpg')">image/jpg</xsl:when>
												<xsl:when test="contains(./value/vertexstyle/@imageURL, 'gif')">image/gif</xsl:when>
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
												<xsl:value-of select="xslutil:toHexColor(./value/fill/color)"/>
											</sld:CssParameter>
										</sld:Fill>
									</sld:Mark>
									<sld:Size>
										<xsl:value-of
											select="./value/vertexstyle/@size" />
									</sld:Size>
								</xsl:otherwise>
							</xsl:choose>
						</sld:Graphic>
					</sld:PointSymbolizer>
				</xsl:when>
				
			</xsl:choose>
		</sld:Rule>
	</xsl:template>
	<xsl:template match="fill" name="fill">
		<sld:Fill>
			<sld:CssParameter name="fill">
				<xsl:value-of select="xslutil:toHexColor(color)"/>
			</sld:CssParameter>
			<sld:CssParameter name="fill-opacity">
				<xsl:value-of select="xslutil:toAlphaValue(color)"/>
			</sld:CssParameter>
			<!--[PENDING: this is the last token of the above] -->
		</sld:Fill>
	</xsl:template>
	<xsl:template match="line" name="stroke">
		<sld:Stroke>
			<sld:CssParameter name="stroke">
				<xsl:value-of select="xslutil:toHexColor(color)"/>
			</sld:CssParameter>
			<sld:CssParameter name="stroke-opacity">
				<xsl:value-of select="xslutil:toAlphaValue(color)"/>
			</sld:CssParameter>
			<sld:CssParameter name="stroke-width">
				<xsl:value-of select="@width"/>
			</sld:CssParameter>
			<sld:CssParameter name="stroke-dasharray">1</sld:CssParameter>
			<!--[PENDING: is this in JUMP?] -->
		</sld:Stroke>
	</xsl:template>
</xsl:stylesheet>
