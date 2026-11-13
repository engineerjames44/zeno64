// usage:
//   swift composite.swift orient <photo> <out.jpg>
//   swift composite.swift crop <img> <x> <y> <w> <h> <out.jpg>        (top-left origin)
//   swift composite.swift comp <photo> <screenshot> <out.jpg> tlx tly trx try brx bry blx bly cropX cropY cropW cropH
import CoreImage
import Foundation
import ImageIO
import UniformTypeIdentifiers

let ctx = CIContext()
let a = CommandLine.arguments

func load(_ p: String) -> CIImage {
    CIImage(contentsOf: URL(fileURLWithPath: p), options: [.applyOrientationProperty: true])!
}
func save(_ img: CIImage, _ p: String) {
    let cs = CGColorSpace(name: CGColorSpace.sRGB)!
    try! ctx.writeJPEGRepresentation(of: img, to: URL(fileURLWithPath: p), colorSpace: cs,
                                     options: [kCGImageDestinationLossyCompressionQuality as CIImageRepresentationOption: 0.92])
}
// top-left-origin crop on an image whose extent starts at 0,0
func crop(_ img: CIImage, _ x: Double, _ y: Double, _ w: Double, _ h: Double) -> CIImage {
    let H = img.extent.height
    return img.cropped(to: CGRect(x: x, y: H - y - h, width: w, height: h))
        .transformed(by: CGAffineTransform(translationX: -x, y: -(H - y - h)))
}

switch a[1] {
case "orient":
    save(load(a[2]), a[3])
case "crop":
    let v = a[3...6].map { Double($0)! }
    save(crop(load(a[2]), v[0], v[1], v[2], v[3]), a[7])
case "comp":
    let photo = load(a[2])
    let H = photo.extent.height
    let v = a[5...16].map { Double($0)! }
    func pt(_ x: Double, _ y: Double) -> CIVector { CIVector(x: x, y: H - y) }

    // The TV screen is a little soft in a phone photo: blur the screenshot slightly and lift its blacks.
    var shot = load(a[3])
    shot = shot.applyingGaussianBlur(sigma: 1.2).cropped(to: shot.extent)
    shot = shot.applyingFilter("CIColorControls", parameters: ["inputContrast": 0.92, "inputSaturation": 1.05, "inputBrightness": 0.0])
    // Match the photo's exposure, and darken the edges like a real backlit panel.
    shot = shot.applyingFilter("CIExposureAdjust", parameters: ["inputEV": -0.55])
    shot = shot.applyingFilter("CIVignette", parameters: ["inputIntensity": 0.45, "inputRadius": 1.6]).cropped(to: shot.extent)

    let warped = shot.applyingFilter("CIPerspectiveTransform", parameters: [
        "inputTopLeft": pt(v[0], v[1]), "inputTopRight": pt(v[2], v[3]),
        "inputBottomRight": pt(v[4], v[5]), "inputBottomLeft": pt(v[6], v[7]),
    ])
    var out = photo  // glow dropped: it smeared past the bezel
    out = warped.composited(over: out).cropped(to: photo.extent)
    // Light sensor noise over everything so the screen and the room share the same grain.
    let noise = CIFilter(name: "CIRandomGenerator")!.outputImage!.cropped(to: photo.extent)
        .applyingFilter("CIColorMatrix", parameters: [
            "inputRVector": CIVector(x: 0, y: 1, z: 0, w: 0), "inputGVector": CIVector(x: 0, y: 1, z: 0, w: 0),
            "inputBVector": CIVector(x: 0, y: 1, z: 0, w: 0), "inputAVector": CIVector(x: 0, y: 0, z: 0, w: 0.035)])
    out = noise.applyingFilter("CISoftLightBlendMode", parameters: [kCIInputBackgroundImageKey: out]).cropped(to: photo.extent)
    save(crop(out, v[8], v[9], v[10], v[11]), a[4])
default:
    print("bad mode")
}
