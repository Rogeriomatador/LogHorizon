# LogHorizonAnyEnchant

Ponte de compatibilidade entre o UberEnchant e os efeitos vanilla do Minecraft 26.2.

O plugin considera os encantamentos vanilla presentes na mão principal, mão secundária e armaduras equipadas. Para cada encantamento, usa o maior nível encontrado e o espelha temporariamente para o item ou slot em que o próprio Minecraft sabe executar esse efeito.

Exemplos:

- Afiação em um peitoral passa a funcionar ao segurar uma espada.
- Poder em uma armadura passa a funcionar ao usar um arco.
- Investida em uma armadura passa a funcionar ao usar uma lança compatível.
- Proteção em uma arma é aplicada a uma única peça de armadura, evitando soma multiplicada.
- Remendo e Durabilidade podem ser propagados para os equipamentos compatíveis.

Os encantamentos sintéticos são identificados por PDC e restaurados ao mover, dropar, morrer, sair ou desligar o plugin. Maldições não são propagadas por padrão.
