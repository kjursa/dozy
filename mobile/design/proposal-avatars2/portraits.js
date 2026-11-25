// Original layered illustrations. Each layer uses the same 120-unit coordinate space.
function drawPortrait(canvas, type, index, color, outfit = 0, hat = 0, layer = "all") {
  const ctx = canvas.getContext("2d");
  ctx.setTransform(canvas.width / 120, 0, 0, canvas.height / 120, 0, 0);
  ctx.clearRect(0, 0, 120, 120);
  ctx.lineWidth = 2.8;
  ctx.lineJoin = "round";
  ctx.lineCap = "round";
  ctx.strokeStyle = "#30252a";

  function paint(fill, outlined) {
    ctx.fillStyle = fill;
    ctx.fill();
    if (outlined) ctx.stroke();
  }

  function oval(x, y, rx, ry, fill, outlined = true) {
    ctx.beginPath();
    ctx.ellipse(x, y, rx, ry, 0, 0, Math.PI * 2);
    paint(fill, outlined);
  }

  function polygon(points, fill, outlined = true) {
    ctx.beginPath();
    points.forEach(([x, y], i) => i ? ctx.lineTo(x, y) : ctx.moveTo(x, y));
    ctx.closePath();
    paint(fill, outlined);
  }

  function face(y, cheekOffset = 17) {
    oval(50, y, 3, 4, "#30252a", false);
    oval(70, y, 3, 4, "#30252a", false);
    oval(60 - cheekOffset, y + 9, 5, 3.5, "#ed8b78", false);
    oval(60 + cheekOffset, y + 9, 5, 3.5, "#ed8b78", false);
    ctx.beginPath();
    ctx.moveTo(55, y + 9);
    ctx.quadraticCurveTo(60, y + 15, 65, y + 9);
    ctx.stroke();
  }

  function drawClothes() {
    const fabric = ["#f2ad28", "#3f83ac", "#a96385", "#56a691"][outfit];
    if (outfit === 0) oval(60, 89, 27, 15, fabric);
    polygon([[27, 116], [31, 97], [50, 88], [70, 88], [89, 97], [93, 116]], fabric);
    if (outfit === 0) {
      polygon([[50, 90], [60, 98], [70, 90], [60, 88]], "#ffd476");
      oval(55, 106, 1.2, 1.2, "#30252a", false);
      oval(65, 106, 1.2, 1.2, "#30252a", false);
    } else if (outfit === 2) {
      polygon([[49, 90], [60, 113], [71, 90], [60, 96]], "#e0aabf");
    } else if (outfit === 3) {
      oval(60, 94, 19, 6, "#ffbd59");
      polygon([[68, 96], [77, 96], [79, 110], [70, 109]], "#ffbd59");
    }
  }

  function drawHead() {
    const hair = ["#75462f", "#4b332f", "#ac6139", "#422e3e", "#9d6038", "#532f25", "#45332e", "#35292f", "#b37c45", "#72422e"][index];
    if (index === 1) oval(60, 56, 34, 37, hair);
    if (index === 5) {
      oval(27, 61, 12, 21, hair);
      oval(93, 61, 12, 21, hair);
    }
    if (index === 3 || index === 7) oval(index === 3 ? 59 : 76, 24, 13, 11, hair);
    oval(60, 86, 9, 10, color);
    oval(30, 61, 7, 8, color);
    oval(90, 61, 7, 8, color);
    oval(60, 58, index === 2 ? 29 : 31, index === 2 ? 30 : 28, color);
    if (index === 0 || index === 4) {
      const curls = [[33, 46], [40, 36], [50, 30], [61, 31], [72, 32], [84, 40]];
      for (const [x, y] of curls) oval(x, y, index === 0 ? 9 : 11, index === 0 ? 9 : 6, hair);
    } else if (index === 6) {
      oval(60, 40, 26, 10, hair);
    } else if (index === 8) {
      oval(60, 42, 30, 16, hair);
    } else if (index === 9) {
      polygon([[32, 47], [38, 34], [49, 24], [53, 35], [67, 22], [70, 35], [83, 30], [88, 48]], hair);
    } else if (index === 3 || index === 7) {
      oval(60, 41, 28, 14, hair);
    } else {
      polygon([[31, 48], [39, 33], [63, 29], [82, 37], [89, 50], [65, 46], [54, 39], [46, 51]], hair);
    }
    face(60, 16);
  }

  function drawHat() {
    if (hat === 1) {
      oval(60, 25, 25, 13, "#e85257");
      oval(73, 35, 26, 4, "#e85257");
    } else if (hat === 2) {
      oval(60, 25, 25, 14, "#9377ba");
      oval(60, 12, 6, 6, "#c0a5e5");
      polygon([[33, 29], [87, 29], [85, 38], [35, 38]], "#b394d6");
    } else if (hat === 3) {
      polygon([[33, 35], [42, 18], [60, 26], [78, 18], [87, 35]], "#36496b");
      oval(60, 32, 6, 5, "#ffc65d");
    }
  }

  function drawCreature() {
    if (type === "animal") {
      if (index === 2) {
        oval(45, 33, 9, 22, color);
        oval(75, 33, 9, 22, color);
        oval(45, 33, 4, 14, "#f1adad", false);
        oval(75, 33, 4, 14, "#f1adad", false);
      } else if (index === 1) {
        oval(33, 58, 12, 21, color);
        oval(87, 58, 12, 21, color);
      } else {
        polygon([[30, 58], [32, 27], [53, 46], [68, 46], [88, 27], [90, 58]], color);
        polygon([[36, 47], [37, 35], [48, 46]], "#f1adad", false);
        polygon([[72, 46], [83, 35], [84, 47]], "#f1adad", false);
      }
    } else if (index === 0) {
      polygon([[35, 92], [13, 77], [24, 61], [46, 76]], "#eb7b6d");
      polygon([[85, 92], [107, 77], [96, 61], [74, 76]], "#eb7b6d");
      polygon([[38, 50], [39, 23], [52, 44], [60, 25], [68, 44], [81, 23], [82, 50]], color);
    } else if (index === 1) {
      polygon([[31, 59], [30, 32], [49, 47], [71, 47], [90, 32], [89, 59]], color);
    } else {
      polygon([[41, 54], [60, 23], [79, 54]], "#ffcb61");
    }
    oval(60, 93, 22, 18, color);
    oval(60, 66, 31, 26, color);
    if (type === "animal" && index === 3) {
      polygon([[33, 67], [60, 77], [87, 67], [78, 85], [60, 92], [42, 85]], "#fff1d9", false);
    }
    face(63);
  }

  if (type !== "person") {
    drawCreature();
    return;
  }

  if (layer === "clothes") {
    ctx.translate(-18, -156);
    ctx.scale(1.3, 2);
    drawClothes();
  } else if (layer === "hat") {
    ctx.translate(0, 28);
    drawHat();
  } else if (layer === "head") {
    ctx.translate(0, -3);
    drawHead();
  } else {
    drawClothes();
    drawHead();
    drawHat();
  }
}
