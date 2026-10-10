const category = (label, dir, items) => ({
  type: 'category',
  label,
  items: items.map(item => (typeof item === 'string' ? `${dir}/${item}` : item)),
});

module.exports = {
  docs: [
    'README',
    category('Core', 'core', [
      'Overview',
      'Diagram',
      category('Classes', 'core/classes', ['Evolution', 'Stage', 'Status', 'Yield']),
    ]),
    category('Stages library', 'lib', [
      'Overview',
      category('Base', 'lib/base', [
        'Overview', 'Alterator', 'BinaryOperator', 'ConstEvolution', 'Fn', 'Fruitful', 'FruitfulStaticStage',
        'Outcome', 'Projection', 'SafeStage', 'SAMStage', 'Stagnation', 'StaticStage',
      ]),
      category('Standard stages', 'lib/std', [
        'Overview', 'Coalesce', 'Complete', 'Const', 'Countdown', 'DeadEnd', 'GlobalSoftDeadline', 'Identity', 'Scan',
        'Swap',
      ]),
      category('Operators', 'lib/operators', [
        'Overview', 'And', 'CompleteIfNone', 'CompleteIfSome', 'IAnd', 'Identity', 'KeepLastOutput', 'Lift',
        'LocalSoftDeadline', 'Or', 'Safe',
      ]),
      category('Cycles', 'lib/cycles', ['Overview', 'Loop', 'Repeat', 'Reduce', 'Fold']),
      category('Projection stages', 'lib/projections', ['Overview', 'Either', 'Tuple2', 'Unlift']),
    ]),
    category('Cats', 'cats', [
      'Overview',
      category('Classes', 'cats/classes', ['IOr', 'StatusInstances', 'Validated']),
    ]),
  ],
};
