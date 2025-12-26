// 1. 核心评分概览数据（数组形式）
const overviewData = [
  { index: 1, indicator: '整体平均分', details: '85.6分', trend: '▲ 较上次实验提升2.1分' },
  { index: 2, indicator: '成绩分布', details: '优秀(23%)·良好(43%)·中等(20%)·待改进(14%)', trend: '正态分布偏右' },
  { index: 3, indicator: '评阅效率', details: '平均24秒/份，最快18秒', trend: '共处理35份报告，总计14分钟' },
  { index: 4, indicator: 'AI置信度', details: '92.3%', trend: '基于内部一致性评估' }
];

// 2. 等级分布数据
const levelData = [
  { name: '优秀', value: 8, rate: '23%' },
  { name: '良好', value: 15, rate: '43%' },
  { name: '中等', value: 7, rate: '20%' },
  { name: '待改进', value: 5, rate: '14%' }
];

// 3. 学生成绩+能力维度+优缺点数据（25组）
const studentData = [
  // 原有10组
  {
    id: 1,
    no: '2021040327',
    name: '李冬强',
    total: 85,
    level: '良好',
    strong: '步骤清晰度',
    weak: '分析深度',
    time: '26s',
    ability: [
      { name: '格式规范性', value: 85 },
      { name: '目的原理', value: 88 },
      { name: '步骤清晰程度', value: 92 },
      { name: '数据充分性', value: 80 },
      { name: '逻辑严谨性', value: 78 },
      { name: '分析深度', value: 70 },
      { name: '创新思维维度', value: 75 }
    ],
    strength: [
      { label: '步骤完整性', rate: '92%' },
      { label: '图表专业化', rate: '85%' },
      { label: '代码质量', rate: '78%' }
    ],
    weakness: [
      { label: '数据结论脱节', rate: '65%' },
      { label: '异常处理缺失', rate: '58%' },
      { label: '分析深度不足', rate: '52%' }
    ]
  },
  {
    id: 2,
    no: '2021040328',
    name: '管某',
    total: 92,
    level: '优秀',
    strong: '创新思维',
    weak: '格式规范',
    time: '31s',
    ability: [
      { name: '格式规范性', value: 70 },
      { name: '目的原理', value: 85 },
      { name: '步骤清晰程度', value: 82 },
      { name: '数据充分性', value: 88 },
      { name: '逻辑严谨性', value: 90 },
      { name: '分析深度', value: 85 },
      { name: '创新思维维度', value: 95 }
    ],
    strength: [
      { label: '创新思维', rate: '95%' },
      { label: '逻辑严谨性', rate: '90%' },
      { label: '数据充分性', rate: '88%' }
    ],
    weakness: [
      { label: '格式规范缺失', rate: '70%' },
      { label: '步骤描述简略', rate: '62%' },
      { label: '异常处理不足', rate: '55%' }
    ]
  },
  {
    id: 3,
    no: '2021040329',
    name: '王明',
    total: 79,
    level: '中等',
    strong: '目的原理',
    weak: '逻辑严谨',
    time: '22s',
    ability: [
      { name: '格式规范性', value: 80 },
      { name: '目的原理', value: 90 },
      { name: '步骤清晰程度', value: 75 },
      { name: '数据充分性', value: 78 },
      { name: '逻辑严谨性', value: 65 },
      { name: '分析深度', value: 72 },
      { name: '创新思维维度', value: 70 }
    ],
    strength: [
      { label: '目的原理理解', rate: '90%' },
      { label: '格式规范性', rate: '80%' },
      { label: '数据充分性', rate: '78%' }
    ],
    weakness: [
      { label: '逻辑严谨性不足', rate: '65%' },
      { label: '步骤清晰度不够', rate: '75%' },
      { label: '创新度不足', rate: '70%' }
    ]
  },
  {
    id: 4,
    no: '2021040330',
    name: '刘芳',
    total: 88,
    level: '良好',
    strong: '数据充分性',
    weak: '创新思维',
    time: '24s',
    ability: [
      { name: '格式规范性', value: 88 },
      { name: '目的原理', value: 85 },
      { name: '步骤清晰程度', value: 82 },
      { name: '数据充分性', value: 93 },
      { name: '逻辑严谨性', value: 85 },
      { name: '分析深度', value: 80 },
      { name: '创新思维维度', value: 68 }
    ],
    strength: [
      { label: '数据充分性', rate: '93%' },
      { label: '格式规范性', rate: '88%' },
      { label: '逻辑严谨性', rate: '85%' }
    ],
    weakness: [
      { label: '创新思维不足', rate: '68%' },
      { label: '分析深度有限', rate: '80%' },
      { label: '步骤描述简略', rate: '82%' }
    ]
  },
  {
    id: 5,
    no: '2021040331',
    name: '张伟',
    total: 76,
    level: '中等',
    strong: '格式规范',
    weak: '分析深度',
    time: '19s',
    ability: [
      { name: '格式规范性', value: 90 },
      { name: '目的原理', value: 75 },
      { name: '步骤清晰程度', value: 78 },
      { name: '数据充分性', value: 72 },
      { name: '逻辑严谨性', value: 70 },
      { name: '分析深度', value: 65 },
      { name: '创新思维维度', value: 70 }
    ],
    strength: [
      { label: '格式规范性', rate: '90%' },
      { label: '步骤清晰度', rate: '78%' },
      { label: '创新思维', rate: '70%' }
    ],
    weakness: [
      { label: '分析深度不足', rate: '65%' },
      { label: '逻辑严谨性不够', rate: '70%' },
      { label: '数据充分性不足', rate: '72%' }
    ]
  },
  {
    id: 6,
    no: '2021040332',
    name: '陈晨',
    total: 94,
    level: '优秀',
    strong: '逻辑严谨',
    weak: '步骤清晰',
    time: '28s',
    ability: [
      { name: '格式规范性', value: 85 },
      { name: '目的原理', value: 90 },
      { name: '步骤清晰程度', value: 75 },
      { name: '数据充分性', value: 92 },
      { name: '逻辑严谨性', value: 95 },
      { name: '分析深度', value: 90 },
      { name: '创新思维维度', value: 88 }
    ],
    strength: [
      { label: '逻辑严谨性', rate: '95%' },
      { label: '数据充分性', rate: '92%' },
      { label: '分析深度', rate: '90%' }
    ],
    weakness: [
      { label: '步骤清晰度不足', rate: '75%' },
      { label: '格式规范小瑕疵', rate: '85%' },
      { label: '创新思维待提升', rate: '88%' }
    ]
  },
  {
    id: 7,
    no: '2021040333',
    name: '赵静',
    total: 81,
    level: '良好',
    strong: '步骤清晰',
    weak: '创新思维',
    time: '23s',
    ability: [
      { name: '格式规范性', value: 82 },
      { name: '目的原理', value: 78 },
      { name: '步骤清晰程度', value: 90 },
      { name: '数据充分性', value: 80 },
      { name: '逻辑严谨性', value: 85 },
      { name: '分析深度', value: 75 },
      { name: '创新思维维度', value: 65 }
    ],
    strength: [
      { label: '步骤清晰度', rate: '90%' },
      { label: '逻辑严谨性', rate: '85%' },
      { label: '格式规范性', rate: '82%' }
    ],
    weakness: [
      { label: '创新思维不足', rate: '65%' },
      { label: '分析深度有限', rate: '75%' },
      { label: '目的原理理解不足', rate: '78%' }
    ]
  },
  {
    id: 8,
    no: '2021040334',
    name: '黄涛',
    total: 69,
    level: '待改进',
    strong: '目的原理',
    weak: '数据充分性',
    time: '32s',
    ability: [
      { name: '格式规范性', value: 70 },
      { name: '目的原理', value: 80 },
      { name: '步骤清晰程度', value: 65 },
      { name: '数据充分性', value: 55 },
      { name: '逻辑严谨性', value: 60 },
      { name: '分析深度', value: 58 },
      { name: '创新思维维度', value: 62 }
    ],
    strength: [
      { label: '目的原理理解', rate: '80%' },
      { label: '创新思维', rate: '62%' },
      { label: '格式规范性', rate: '70%' }
    ],
    weakness: [
      { label: '数据充分性不足', rate: '55%' },
      { label: '分析深度不够', rate: '58%' },
      { label: '逻辑严谨性缺失', rate: '60%' }
    ]
  },
  {
    id: 9,
    no: '2021040335',
    name: '周琳',
    total: 87,
    level: '良好',
    strong: '格式规范',
    weak: '逻辑严谨',
    time: '25s',
    ability: [
      { name: '格式规范性', value: 92 },
      { name: '目的原理', value: 85 },
      { name: '步骤清晰程度', value: 88 },
      { name: '数据充分性', value: 82 },
      { name: '逻辑严谨性', value: 70 },
      { name: '分析深度', value: 80 },
      { name: '创新思维维度', value: 75 }
    ],
    strength: [
      { label: '格式规范性', rate: '92%' },
      { label: '步骤清晰度', rate: '88%' },
      { label: '目的原理', rate: '85%' }
    ],
    weakness: [
      { label: '逻辑严谨性不足', rate: '70%' },
      { label: '创新思维有限', rate: '75%' },
      { label: '数据充分性待提升', rate: '82%' }
    ]
  },
  {
    id: 10,
    no: '2021040336',
    name: '吴翔',
    total: 90,
    level: '优秀',
    strong: '分析深度',
    weak: '步骤清晰',
    time: '27s',
    ability: [
      { name: '格式规范性', value: 85 },
      { name: '目的原理', value: 88 },
      { name: '步骤清晰程度', value: 72 },
      { name: '数据充分性', value: 90 },
      { name: '逻辑严谨性', value: 85 },
      { name: '分析深度', value: 95 },
      { name: '创新思维维度', value: 82 }
    ],
    strength: [
      { label: '分析深度', rate: '95%' },
      { label: '数据充分性', rate: '90%' },
      { label: '目的原理', rate: '88%' }
    ],
    weakness: [
      { label: '步骤清晰度不足', rate: '72%' },
      { label: '创新思维待提升', rate: '82%' },
      { label: '格式规范小瑕疵', rate: '85%' }
    ]
  },
  // 新增15组（11-25）
  {
    id: 11,
    no: '2021040337',
    name: '郑浩',
    total: 83,
    level: '良好',
    strong: '数据充分性',
    weak: '分析深度',
    time: '22s',
    ability: [
      { name: '格式规范性', value: 80 },
      { name: '目的原理', value: 82 },
      { name: '步骤清晰程度', value: 85 },
      { name: '数据充分性', value: 90 },
      { name: '逻辑严谨性', value: 78 },
      { name: '分析深度', value: 70 },
      { name: '创新思维维度', value: 75 }
    ],
    strength: [
      { label: '数据充分性', rate: '90%' },
      { label: '步骤清晰度', rate: '85%' },
      { label: '目的原理', rate: '82%' }
    ],
    weakness: [
      { label: '分析深度不足', rate: '70%' },
      { label: '逻辑严谨性不够', rate: '78%' },
      { label: '格式规范小瑕疵', rate: '80%' }
    ]
  },
  {
    id: 12,
    no: '2021040338',
    name: '孙悦',
    total: 91,
    level: '优秀',
    strong: '创新思维',
    weak: '步骤清晰',
    time: '30s',
    ability: [
      { name: '格式规范性', value: 85 },
      { name: '目的原理', value: 88 },
      { name: '步骤清晰程度', value: 75 },
      { name: '数据充分性', value: 85 },
      { name: '逻辑严谨性', value: 90 },
      { name: '分析深度', value: 88 },
      { name: '创新思维维度', value: 92 }
    ],
    strength: [
      { label: '创新思维', rate: '92%' },
      { label: '逻辑严谨性', rate: '90%' },
      { label: '分析深度', rate: '88%' }
    ],
    weakness: [
      { label: '步骤清晰度不足', rate: '75%' },
      { label: '数据充分性待提升', rate: '85%' },
      { label: '格式规范小瑕疵', rate: '85%' }
    ]
  },
  {
    id: 13,
    no: '2021040339',
    name: '马丽',
    total: 77,
    level: '中等',
    strong: '格式规范',
    weak: '逻辑严谨',
    time: '21s',
    ability: [
      { name: '格式规范性', value: 88 },
      { name: '目的原理', value: 75 },
      { name: '步骤清晰程度', value: 78 },
      { name: '数据充分性', value: 76 },
      { name: '逻辑严谨性', value: 68 },
      { name: '分析深度', value: 72 },
      { name: '创新思维维度', value: 70 }
    ],
    strength: [
      { label: '格式规范性', rate: '88%' },
      { label: '步骤清晰度', rate: '78%' },
      { label: '数据充分性', rate: '76%' }
    ],
    weakness: [
      { label: '逻辑严谨性不足', rate: '68%' },
      { label: '目的原理理解不足', rate: '75%' },
      { label: '创新度不足', rate: '70%' }
    ]
  },
  {
    id: 14,
    no: '2021040340',
    name: '朱军',
    total: 84,
    level: '良好',
    strong: '步骤清晰',
    weak: '创新思维',
    time: '24s',
    ability: [
      { name: '格式规范性', value: 82 },
      { name: '目的原理', value: 80 },
      { name: '步骤清晰程度', value: 90 },
      { name: '数据充分性', value: 85 },
      { name: '逻辑严谨性', value: 82 },
      { name: '分析深度', value: 78 },
      { name: '创新思维维度', value: 65 }
    ],
    strength: [
      { label: '步骤清晰度', rate: '90%' },
      { label: '数据充分性', rate: '85%' },
      { label: '逻辑严谨性', rate: '82%' }
    ],
    weakness: [
      { label: '创新思维不足', rate: '65%' },
      { label: '分析深度有限', rate: '78%' },
      { label: '目的原理理解不足', rate: '80%' }
    ]
  },
  {
    id: 15,
    no: '2021040341',
    name: '林晓',
    total: 75,
    level: '中等',
    strong: '目的原理',
    weak: '数据充分性',
    time: '19s',
    ability: [
      { name: '格式规范性', value: 80 },
      { name: '目的原理', value: 85 },
      { name: '步骤清晰程度', value: 72 },
      { name: '数据充分性', value: 65 },
      { name: '逻辑严谨性', value: 70 },
      { name: '分析深度', value: 75 },
      { name: '创新思维维度', value: 72 }
    ],
    strength: [
      { label: '目的原理理解', rate: '85%' },
      { label: '分析深度', rate: '75%' },
      { label: '格式规范性', rate: '80%' }
    ],
    weakness: [
      { label: '数据充分性不足', rate: '65%' },
      { label: '逻辑严谨性不够', rate: '70%' },
      { label: '步骤清晰度不足', rate: '72%' }
    ]
  },
  {
    id: 16,
    no: '2021040342',
    name: '郭阳',
    total: 93,
    level: '优秀',
    strong: '分析深度',
    weak: '格式规范',
    time: '29s',
    ability: [
      { name: '格式规范性', value: 78 },
      { name: '目的原理', value: 90 },
      { name: '步骤清晰程度', value: 85 },
      { name: '数据充分性', value: 92 },
      { name: '逻辑严谨性', value: 90 },
      { name: '分析深度', value: 95 },
      { name: '创新思维维度', value: 88 }
    ],
    strength: [
      { label: '分析深度', rate: '95%' },
      { label: '数据充分性', rate: '92%' },
      { label: '目的原理', rate: '90%' }
    ],
    weakness: [
      { label: '格式规范小瑕疵', rate: '78%' },
      { label: '步骤清晰度待提升', rate: '85%' },
      { label: '创新思维待提升', rate: '88%' }
    ]
  },
  {
    id: 17,
    no: '2021040343',
    name: '何佳',
    total: 80,
    level: '良好',
    strong: '逻辑严谨',
    weak: '分析深度',
    time: '23s',
    ability: [
      { name: '格式规范性', value: 85 },
      { name: '目的原理', value: 78 },
      { name: '步骤清晰程度', value: 82 },
      { name: '数据充分性', value: 80 },
      { name: '逻辑严谨性', value: 88 },
      { name: '分析深度', value: 72 },
      { name: '创新思维维度', value: 75 }
    ],
    strength: [
      { label: '逻辑严谨性', rate: '88%' },
      { label: '格式规范性', rate: '85%' },
      { label: '步骤清晰度', rate: '82%' }
    ],
    weakness: [
      { label: '分析深度不足', rate: '72%' },
      { label: '目的原理理解不足', rate: '78%' },
      { label: '创新思维有限', rate: '75%' }
    ]
  },
  {
    id: 18,
    no: '2021040344',
    name: '高鑫',
    total: 68,
    level: '待改进',
    strong: '格式规范',
    weak: '数据充分性',
    time: '31s',
    ability: [
      { name: '格式规范性', value: 80 },
      { name: '目的原理', value: 70 },
      { name: '步骤清晰程度', value: 65 },
      { name: '数据充分性', value: 52 },
      { name: '逻辑严谨性', value: 58 },
      { name: '分析深度', value: 60 },
      { name: '创新思维维度', value: 65 }
    ],
    strength: [
      { label: '格式规范性', rate: '80%' },
      { label: '创新思维', rate: '65%' },
      { label: '分析深度', rate: '60%' }
    ],
    weakness: [
      { label: '数据充分性不足', rate: '52%' },
      { label: '逻辑严谨性缺失', rate: '58%' },
      { label: '步骤清晰度不足', rate: '65%' }
    ]
  },
  {
    id: 19,
    no: '2021040345',
    name: '罗琦',
    total: 86,
    level: '良好',
    strong: '数据充分性',
    weak: '创新思维',
    time: '25s',
    ability: [
      { name: '格式规范性', value: 82 },
      { name: '目的原理', value: 85 },
      { name: '步骤清晰程度', value: 88 },
      { name: '数据充分性', value: 90 },
      { name: '逻辑严谨性', value: 85 },
      { name: '分析深度', value: 80 },
      { name: '创新思维维度', value: 68 }
    ],
    strength: [
      { label: '数据充分性', rate: '90%' },
      { label: '步骤清晰度', rate: '88%' },
      { label: '目的原理', rate: '85%' }
    ],
    weakness: [
      { label: '创新思维不足', rate: '68%' },
      { label: '分析深度有限', rate: '80%' },
      { label: '格式规范小瑕疵', rate: '82%' }
    ]
  },
  {
    id: 20,
    no: '2021040346',
    name: '梁超',
    total: 89,
    level: '优秀',
    strong: '逻辑严谨',
    weak: '步骤清晰',
    time: '27s',
    ability: [
      { name: '格式规范性', value: 85 },
      { name: '目的原理', value: 88 },
      { name: '步骤清晰程度', value: 75 },
      { name: '数据充分性', value: 88 },
      { name: '逻辑严谨性', value: 92 },
      { name: '分析深度', value: 85 },
      { name: '创新思维维度', value: 80 }
    ],
    strength: [
      { label: '逻辑严谨性', rate: '92%' },
      { label: '数据充分性', rate: '88%' },
      { label: '目的原理', rate: '88%' }
    ],
    weakness: [
      { label: '步骤清晰度不足', rate: '75%' },
      { label: '创新思维待提升', rate: '80%' },
      { label: '分析深度待提升', rate: '85%' }
    ]
  },
  {
    id: 21,
    no: '2021040347',
    name: '宋佳',
    total: 74,
    level: '中等',
    strong: '目的原理',
    weak: '分析深度',
    time: '20s',
    ability: [
      { name: '格式规范性', value: 78 },
      { name: '目的原理', value: 85 },
      { name: '步骤清晰程度', value: 72 },
      { name: '数据充分性', value: 70 },
      { name: '逻辑严谨性', value: 68 },
      { name: '分析深度', value: 65 },
      { name: '创新思维维度', value: 70 }
    ],
    strength: [
      { label: '目的原理理解', rate: '85%' },
      { label: '格式规范性', rate: '78%' },
      { label: '创新思维', rate: '70%' }
    ],
    weakness: [
      { label: '分析深度不足', rate: '65%' },
      { label: '逻辑严谨性不够', rate: '68%' },
      { label: '数据充分性不足', rate: '70%' }
    ]
  },
  {
    id: 22,
    no: '2021040348',
    name: '韩磊',
    total: 82,
    level: '良好',
    strong: '步骤清晰',
    weak: '创新思维',
    time: '22s',
    ability: [
      { name: '格式规范性', value: 80 },
      { name: '目的原理', value: 78 },
      { name: '步骤清晰程度', value: 88 },
      { name: '数据充分性', value: 82 },
      { name: '逻辑严谨性', value: 80 },
      { name: '分析深度', value: 75 },
      { name: '创新思维维度', value: 62 }
    ],
    strength: [
      { label: '步骤清晰度', rate: '88%' },
      { label: '数据充分性', rate: '82%' },
      { label: '逻辑严谨性', rate: '80%' }
    ],
    weakness: [
      { label: '创新思维不足', rate: '62%' },
      { label: '分析深度有限', rate: '75%' },
      { label: '目的原理理解不足', rate: '78%' }
    ]
  },
  {
    id: 23,
    no: '2021040349',
    name: '唐欣',
    total: 90,
    level: '优秀',
    strong: '创新思维',
    weak: '格式规范',
    time: '28s',
    ability: [
      { name: '格式规范性', value: 75 },
      { name: '目的原理', value: 85 },
      { name: '步骤清晰程度', value: 82 },
      { name: '数据充分性', value: 88 },
      { name: '逻辑严谨性', value: 85 },
      { name: '分析深度', value: 88 },
      { name: '创新思维维度', value: 93 }
    ],
    strength: [
      { label: '创新思维', rate: '93%' },
      { label: '分析深度', rate: '88%' },
      { label: '数据充分性', rate: '88%' }
    ],
    weakness: [
      { label: '格式规范小瑕疵', rate: '75%' },
      { label: '步骤清晰度待提升', rate: '82%' },
      { label: '逻辑严谨性待提升', rate: '85%' }
    ]
  },
  {
    id: 24,
    no: '2021040350',
    name: '冯宇',
    total: 71,
    level: '中等',
    strong: '格式规范',
    weak: '数据充分性',
    time: '18s',
    ability: [
      { name: '格式规范性', value: 85 },
      { name: '目的原理', value: 72 },
      { name: '步骤清晰程度', value: 70 },
      { name: '数据充分性', value: 60 },
      { name: '逻辑严谨性', value: 65 },
      { name: '分析深度', value: 70 },
      { name: '创新思维维度', value: 68 }
    ],
    strength: [
      { label: '格式规范性', rate: '85%' },
      { label: '分析深度', rate: '70%' },
      { label: '创新思维', rate: '68%' }
    ],
    weakness: [
      { label: '数据充分性不足', rate: '60%' },
      { label: '逻辑严谨性不够', rate: '65%' },
      { label: '步骤清晰度不足', rate: '70%' }
    ]
  },
  {
    id: 25,
    no: '2021040351',
    name: '陈宇',
    total: 87,
    level: '良好',
    strong: '分析深度',
    weak: '步骤清晰',
    time: '26s',
    ability: [
      { name: '格式规范性', value: 82 },
      { name: '目的原理', value: 85 },
      { name: '步骤清晰程度', value: 70 },
      { name: '数据充分性', value: 88 },
      { name: '逻辑严谨性', value: 82 },
      { name: '分析深度', value: 90 },
      { name: '创新思维维度', value: 75 }
    ],
    strength: [
      { label: '分析深度', rate: '90%' },
      { label: '数据充分性', rate: '88%' },
      { label: '目的原理', rate: '85%' }
    ],
    weakness: [
      { label: '步骤清晰度不足', rate: '70%' },
      { label: '创新思维有限', rate: '75%' },
      { label: '逻辑严谨性待提升', rate: '82%' }
    ]
  }
];

// 分页配置
const PAGE_SIZE = 10; // 每页显示10条
let currentPage = 1; // 当前页

// 渲染核心评分概览表
function renderOverviewTable() {
  const table = document.getElementById('overviewTable');
  const thead = document.createElement('tr');
  thead.innerHTML = `
    <td>指标</td>
    <td>详情</td>
    <td>趋势</td>
  `;
  table.appendChild(thead);
  overviewData.forEach(item => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${item.indicator}</td>
      <td>${item.details}</td>
      <td>${item.trend}</td>
    `;
    table.appendChild(tr);
  });
}

// 渲染指定页的成绩表格数据
function renderScoreTable(page) {
  const tbody = document.getElementById('scoreTableBody');
  tbody.innerHTML = '';
  // 计算当前页的起始和结束索引
  const startIndex = (page - 1) * PAGE_SIZE;
  const endIndex = Math.min(startIndex + PAGE_SIZE, studentData.length);
  // 获取当前页的数据
  const currentPageData = studentData.slice(startIndex, endIndex);

  currentPageData.forEach(item => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${item.id}</td>
      <td>${item.no}</td>
      <td class="student-name-link" data-id="${item.id}">${item.name}</td>
      <td>${item.total}</td>
      <td>${item.level}</td>
      <td>${item.strong}</td>
      <td>${item.weak}</td>
      <td>${item.time}</td>
    `;
    // 绑定姓名点击事件
    const nameCell = tr.querySelector('.student-name-link');
    nameCell.addEventListener('click', () => {
      const studentId = parseInt(nameCell.getAttribute('data-id'));
      loadStudentAbilityData(studentId);
    });
    tbody.appendChild(tr);
  });

  // 更新分页信息
  updatePagination(page);
}

// 更新分页控件状态和信息
function updatePagination(page) {
  const totalPages = Math.ceil(studentData.length / PAGE_SIZE); // 总页数
  const prevBtn = document.getElementById('prevPage');
  const nextBtn = document.getElementById('nextPage');
  const pageInfo = document.getElementById('pageInfo');

  // 更新分页文本
  pageInfo.textContent = `第 ${page} 页 / 共 ${totalPages} 页`;

  // 更新按钮禁用状态
  prevBtn.disabled = page === 1;
  nextBtn.disabled = page === totalPages;

  // 保存当前页
  currentPage = page;
}

// 加载学生能力维度数据（点击姓名后触发）
function loadStudentAbilityData(studentId) {
  // 找到对应学生数据
  const student = studentData.find(item => item.id === studentId);
  if (!student) return;

  // 显示学生姓名
  document.getElementById('studentName').textContent = `【${student.name}】`;

  // 渲染能力维度图表
  renderAbilityChart(student.ability);

  // 渲染优缺点卡片
  renderStrengthWeakness(student.strength, student.weakness);
}

// 渲染能力维度扇形图
function renderAbilityChart(abilityData) {
  const chartDom = document.getElementById('abilityChart');
  const myChart = echarts.init(chartDom);
  const option = {
    title: { text: '能力维度评分分布', left: 'center', textStyle: { fontSize: 16, color: '#2c3e50' } },
    tooltip: { trigger: 'item', formatter: '{b}: {c}分' },
    series: [
      {
        name: '分数',
        type: 'pie',
        radius: ['45%', '75%'],
        data: abilityData,
        label: {
          show: true,
          formatter: '{b}: {c}分',
          fontSize: 12,
          color: '#444'
        },
        itemStyle: {
          // 渐变色配色
          color: function (params) {
            const colors = ['#5e7ce2', '#63b3ed', '#68d9a4', '#f0b442', '#e57373', '#a5673f', '#9c27b0'];
            return colors[params.dataIndex % colors.length];
          },
          borderWidth: 2,
          borderColor: '#fff' // 扇形块间加白色边框，更清晰
        }
      }
    ]
  };
  myChart.setOption(option);
  // 窗口resize适配
  window.addEventListener('resize', () => myChart.resize());
}

// 渲染优缺点卡片
function renderStrengthWeakness(strengthData, weaknessData) {
  const container = document.getElementById('strengthWeakness');
  container.innerHTML = '';

  // 高频改进点（缺点）
  const weaknessCard = document.createElement('div');
  weaknessCard.className = 'card';
  weaknessCard.innerHTML = `<h3>高频改进点</h3>`;
  weaknessData.forEach(item => {
    weaknessCard.innerHTML += `<div class="tag">${item.label} ${item.rate}</div>`;
  });
  container.appendChild(weaknessCard);

  // 典型优点
  const strengthCard = document.createElement('div');
  strengthCard.className = 'card';
  strengthCard.innerHTML = `<h3>典型优点</h3>`;
  strengthData.forEach(item => {
    strengthCard.innerHTML += `<div class="tag">${item.label} ${item.rate}</div>`;
  });
  container.appendChild(strengthCard);
}

// 渲染等级分布柱状图
function renderLevelChart() {
  const chartDom = document.getElementById('levelChart');
  const myChart = echarts.init(chartDom);
  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', axisLine: { show: false }, splitLine: { show: false } },
    yAxis: {
      type: 'category',
      data: levelData.map(item => `${item.name}(${item.rate})`),
      axisLine: { show: false },
      axisTick: { show: false },
      textStyle: { color: '#64748b' }
    },
    series: [
      {
        name: '人数',
        type: 'bar',
        barWidth: '60%', // 柱子宽度更协调
        data: levelData.map(item => item.value),
        label: { show: true, position: 'right', fontSize: 12, color: '#2c3e50' },
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#5e7ce2' },
            { offset: 1, color: '#8098f9' }
          ]) // 横向渐变色
        }
      }
    ],
    grid: { left: '10px', right: '40px', top: '20px', bottom: '10px' } // 图表内边距
  };
  myChart.setOption(option);
  window.addEventListener('resize', () => myChart.resize());
}

// 绑定分页按钮事件
function bindPaginationEvents() {
  const prevBtn = document.getElementById('prevPage');
  const nextBtn = document.getElementById('nextPage');

  // 上一页
  prevBtn.addEventListener('click', () => {
    if (currentPage > 1) {
      renderScoreTable(currentPage - 1);
    }
  });

  // 下一页
  nextBtn.addEventListener('click', () => {
    const totalPages = Math.ceil(studentData.length / PAGE_SIZE);
    if (currentPage < totalPages) {
      renderScoreTable(currentPage + 1);
    }
  });
}

// 初始化页面
function init() {
  renderOverviewTable();
  renderScoreTable(1); // 渲染第一页数据
  renderLevelChart();
  bindPaginationEvents(); // 绑定分页事件
  // 初始状态：优缺点区域为空（提示在section-header）
  document.getElementById('strengthWeakness').innerHTML = '';
}

// 页面加载完成后初始化
window.onload = init;