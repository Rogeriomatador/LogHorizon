# Impede repeticao deste reparo no mesmo Exit Portal.
tag @s add loghorizon.b4.dragon_bootstrap_done

# Mantem o sistema oficial de dragao do Stellarity habilitado.
scoreboard players set #stellarity.config stellarity.config.enable_ender_dragon 1

# Reproduz exatamente o estado que foi validado manualmente no servidor.
tag @s add stellarity.in_dragon_fight

# Invoca o dragao OFICIAL do Stellarity, nunca um Ender Dragon vanilla.
execute positioned 0 128 0 run function stellarity:entity/dragon/spawn/summon
