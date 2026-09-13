LOG HORIZON - STELLARITY B4 DRAGON BOOTSTRAP FIX

STATUS AO CRIAR: PREPARED / NOT_EXECUTED

OBJETIVO
Corrigir somente o PRIMEIRO spawn automatico do Ender Dragon customizado do Stellarity em um End novo.

EVIDENCIAS CONFIRMADAS ANTES DESTE PATCH
- O dragao vanilla e removido propositalmente pelo Stellarity.
- `function stellarity:entity/dragon/spawn/summon` criou corretamente o dragao customizado.
- O dragao customizado permaneceu vivo.
- A morte dele concluiu a luta e registrou `stellarity.dragon.times_killed = 1`.
- Quatro End Crystals iniciaram corretamente o respawn do Stellarity.
- `stellarity.config.enable_ender_dragon = 1` foi criado e validado no mundo principal.

O QUE ESTE DATAPACK FAZ
- Mantem `stellarity.config.enable_ender_dragon` em 1 ao carregar.
- Espera existir o marker `stellarity.exit_portal` inicializado.
- Espera um jogador estar a ate 256 blocos do portal central.
- Espera nao existir nenhum Ender Dragon na dimensao.
- So age se ainda nao houve nenhuma morte registrada do dragao.
- So age se nenhuma luta/animacao de morte estiver marcada em andamento.
- Adiciona `stellarity.in_dragon_fight`.
- Chama a funcao oficial `stellarity:entity/dragon/spawn/summon`.
- Marca o portal com `loghorizon.b4.dragon_bootstrap_done` para nao repetir o reparo.

NAO SUBSTITUI
- luta do dragao;
- morte do dragao;
- portal de saida;
- drops;
- bossbars;
- respawn com 4 cristais;
- lutas posteriores.
Tudo isso continua sendo controlado pelo Stellarity.

INSTALACAO
1. Coloque o ZIP em `/home/container/world/datapacks/`.
2. Desligue totalmente o servidor.
3. Apague somente `/home/container/world_the_end/`.
4. Nao apague `/home/container/world/`, `/home/container/world_nether/` ou `/home/container/world/dimensions/`.
5. Ligue o servidor.
6. Rode `/datapack list enabled` e confirme que B4 aparece.
7. Entre no End por um End Portal real. Nao use `/summon`.

TESTE ESPERADO
Primeira entrada -> dragao vanilla e removido pelo Stellarity -> B4 detecta a luta sem boss -> chama o spawn oficial -> dragao Stellarity aparece e permanece vivo.

Enquanto esse teste real nao for feito, o status deste arquivo e NOT_EXECUTED.
