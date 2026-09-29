import { Injectable } from '@angular/core';

export interface Point {
    x: number;
    y: number;
}

export interface TransformState {
    x: number;
    y: number;
    scale: number;
}

/**
 * Service for SVG coordinate transformations and geometry calculations.
 * Extracted from mobile-map-page and floor-plan-editor to avoid code duplication.
 */
@Injectable({
    providedIn: 'root',
})
export class SvgUtilityService {
    /**
     * Convert screen coordinates to SVG coordinates, accounting for zoom and pan transformations.
     * @param svg The SVG element
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
     * @param transform Current transformation state (zoom/pan)
     * @returns SVG coordinates
     */
    public screenToSVGPoint(svg: SVGSVGElement, screenX: number, screenY: number, transform: TransformState): Point {
        const pt = svg.createSVGPoint();
        pt.x = screenX;
        pt.y = screenY;

        // Convert screen coordinates to SVG viewport coordinates
        const svgP = pt.matrixTransform(svg.getScreenCTM()!.inverse());

        // Account for manual transform (zoom/pan)
        const x = (svgP.x - transform.x) / transform.scale;
        const y = (svgP.y - transform.y) / transform.scale;

        return { x, y };
    }

    /**
     * Convert SVG coordinates to screen coordinates.
     * @param svg The SVG element
     * @param svgX SVG X coordinate
     * @param svgY SVG Y coordinate
     * @param transform Current transformation state (zoom/pan)
     * @returns Screen coordinates
     */
    public svgToScreenPoint(svg: SVGSVGElement, svgX: number, svgY: number, transform: TransformState): Point {
        // Apply manual transform (zoom/pan)
        const transformedX = svgX * transform.scale + transform.x;
        const transformedY = svgY * transform.scale + transform.y;

        const pt = svg.createSVGPoint();
        pt.x = transformedX;
        pt.y = transformedY;

        // Convert SVG viewport coordinates to screen coordinates
        const screenP = pt.matrixTransform(svg.getScreenCTM()!);

        return { x: screenP.x, y: screenP.y };
    }

    /**
     * Convert an array of points to SVG polygon string format.
     * @param points Array of points
     * @returns Polygon string (e.g., "0,0 10,0 10,10 0,10")
     */
    public pointsToPolygonString(points: Point[]): string {
        return points.map((p) => `${p.x},${p.y}`).join(' ');
    }

    /**
     * Calculate the center X coordinate of a set of points.
     * @param points Array of points
     * @returns Center X coordinate
     */
    public getCenterX(points: Point[]): number {
        if (points.length === 0) {
            return 0;
        }
        return points.reduce((sum, p) => sum + p.x, 0) / points.length;
    }

    /**
     * Calculate the center Y coordinate of a set of points.
     * @param points Array of points
     * @returns Center Y coordinate
     */
    public getCenterY(points: Point[]): number {
        if (points.length === 0) {
            return 0;
        }
        return points.reduce((sum, p) => sum + p.y, 0) / points.length;
    }

    /**
     * Calculate the center point of a set of points.
     * @param points Array of points
     * @returns Center point
     */
    public getCenterPoint(points: Point[]): Point {
        return {
            x: this.getCenterX(points),
            y: this.getCenterY(points),
        };
    }

    /**
     * Check if a point is inside a polygon using ray casting algorithm.
     * @param point Point to check
     * @param polygon Array of polygon vertices
     * @returns true if point is inside polygon
     */
    public isPointInPolygon(point: Point, polygon: Point[]): boolean {
        let inside = false;
        for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
            const xi = polygon[i].x;
            const yi = polygon[i].y;
            const xj = polygon[j].x;
            const yj = polygon[j].y;

            const intersect = yi > point.y !== yj > point.y && point.x < ((xj - xi) * (point.y - yi)) / (yj - yi) + xi;
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * Calculate distance between two points.
     * @param p1 First point
     * @param p2 Second point
     * @returns Distance
     */
    public distance(p1: Point, p2: Point): number {
        const dx = p2.x - p1.x;
        const dy = p2.y - p1.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Generate SVG transform string from transform state.
     * @param transform Transform state
     * @returns SVG transform string
     */
    public getTransformString(transform: TransformState): string {
        return `translate(${transform.x}, ${transform.y}) scale(${transform.scale})`;
    }
}
