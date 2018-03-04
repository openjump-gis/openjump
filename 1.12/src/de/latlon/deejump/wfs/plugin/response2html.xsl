<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:wfs="http://www.opengis.net/wfs"
	xmlns:ogc="http://www.opengis.net/ogc">
	<xsl:output method="html" />

	<xsl:template match="/">
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="wfs:TransactionSummary">
		<table>
			<tbody>
				<tr>
					<th></th>
					<th>Total</th>
				</tr>
				<tr>
					<td>
						<b>Inserted</b>
					</td>
					<td>
						<xsl:value-of select="./wfs:totalInserted" />
					</td>
				</tr>
				<tr>
					<td>
						<b>Updated</b>
					</td>
					<td>
						<xsl:value-of select="./wfs:totalUpdated" />
					</td>
				</tr>
				<tr>
					<td>
						<b>Deleted</b>
					</td>
					<td>
						<xsl:value-of select="./wfs:totalDeleted" />
					</td>
				</tr>
			</tbody>
		</table>
	</xsl:template>

	<!-- could list all involved features TODO need more working here 	 -->
	<xsl:template match="wfs:InsertResults">
		<p>
			<b>InsertResults</b>
			<br></br>
			<xsl:for-each select="./wfs:Feature/ogc:FeatureId/@fid">
				Feature Id:
				<xsl:value-of select="." />
			</xsl:for-each>
		</p>

	</xsl:template>

	<xsl:template match="ServiceExceptionReport">
		<b>Error:</b>
		<br></br>
		<xsl:value-of select="ServiceException" />
	</xsl:template>
</xsl:stylesheet>
